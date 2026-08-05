package org.carlospinan.bloqueador.app.rules

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.carlospinan.bloqueador.app.db.AppDatabase

class SqlCallLogRepository(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : CallLogRepository {
    private val queries get() = database.appDatabaseQueries

    override fun allEntries(): Flow<List<CallLogEntryData>> =
        queries
            .selectAllCallLogEntries()
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.map { it.toData() } }

    override fun recentEntries(limit: Int): Flow<List<CallLogEntryData>> =
        queries
            .selectRecentCallLogEntries(limit.toLong())
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.map { it.toData() } }

    // An indexed COUNT over a since-0 window: cheap to run, and SQLDelight re-runs it on every
    // CallLogEntry write, which is the notification we actually want. Collecting allEntries()
    // for the same purpose would materialise the whole log on each new call.
    override fun changes(): Flow<Unit> =
        queries
            .countBlockedCallsSince(0L)
            .asFlow()
            .mapToOne(dispatcher)
            .map { }

    override suspend fun blockedCountToday(): Int = blockedCountSince(StatsWindows.startOfToday(currentTimeMillis()))

    override suspend fun blockedCountThisWeek(): Int = blockedCountSince(StatsWindows.startOfWeek(currentTimeMillis()))

    override suspend fun blockedCountThisMonth(): Int = blockedCountSince(StatsWindows.startOfMonth(currentTimeMillis()))

    override suspend fun blockedStats(): BlockedStats =
        withContext(dispatcher) {
            // One "now" for all four, so the counters can't straddle a midnight that falls
            // between two of the queries and report a week smaller than the day inside it.
            val now = currentTimeMillis()
            BlockedStats(
                today = queries.countBlockedCallsSince(StatsWindows.startOfToday(now)).executeAsOne().toInt(),
                thisWeek = queries.countBlockedCallsSince(StatsWindows.startOfWeek(now)).executeAsOne().toInt(),
                thisMonth = queries.countBlockedCallsSince(StatsWindows.startOfMonth(now)).executeAsOne().toInt(),
                pendingReview = queries.countPendingReview().executeAsOne().toInt(),
            )
        }

    private suspend fun blockedCountSince(sinceMillis: Long): Int =
        withContext(dispatcher) {
            queries.countBlockedCallsSince(sinceMillis).executeAsOne().toInt()
        }

    override suspend fun logCall(
        number: String,
        timestamp: Long,
        decision: RuleDecision,
    ) {
        withContext(dispatcher) {
            queries
                .insertCallLogEntry(
                    number = number,
                    timestamp = timestamp,
                    action = if (decision.isBlocked) "BLOCKED" else "ALLOWED",
                    rule_type = decision.ruleTypeTag,
                    rule_id = decision.loggedRuleId,
                    rule_detail = decision.loggedDetail,
                ).value
        }
    }

    override suspend fun clearAll() {
        withContext(dispatcher) {
            queries.clearCallLog().value
        }
    }

    /**
     * Blocked-call counts for the last [daysBack] **local** calendar days, newest first.
     *
     * Buckets are consecutive local midnights, the same boundary [blockedStats] uses, so the
     * first bucket's count always equals [BlockedStats.today]. They are not `now` minus a
     * multiple of 86 400 000 ms: that both drifts off local midnight and mis-sizes the 23- and
     * 25-hour days around a DST change, putting calls in the wrong bucket twice a year.
     *
     * Only blocked timestamps back to the oldest bucket are read; this used to load every
     * column of every row the call log had ever held in order to count seven days.
     */
    override suspend fun blockedByDay(daysBack: Int): List<DayStat> =
        withContext(dispatcher) {
            val buckets = StatsWindows.dayBuckets(currentTimeMillis(), daysBack)
            val oldest = buckets.lastOrNull()?.startMillis ?: return@withContext emptyList()
            val timestamps = queries.selectBlockedTimestampsSince(oldest).executeAsList()
            buckets.map { bucket ->
                DayStat(
                    daysAgo = bucket.daysAgo,
                    count = timestamps.count { it >= bucket.startMillis && it < bucket.endMillis },
                    cutoffEpochMillis = bucket.startMillis,
                )
            }
        }

    private fun org.carlospinan.bloqueador.app.db.CallLogEntry.toData() =
        CallLogEntryData(
            id = id,
            number = number,
            timestamp = timestamp,
            action = action,
            ruleType = rule_type,
            ruleId = rule_id,
            ruleDetail = rule_detail,
        )
}
