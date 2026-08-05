package org.carlospinan.bloqueador.app.rules

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
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

    override suspend fun blockedCountToday(): Int =
        withContext(dispatcher) {
            queries.countBlockedCallsToday().executeAsOne().toInt()
        }

    override suspend fun blockedCountThisWeek(): Int =
        withContext(dispatcher) {
            queries.countBlockedCallsThisWeek().executeAsOne().toInt()
        }

    override suspend fun blockedCountThisMonth(): Int =
        withContext(dispatcher) {
            queries.countBlockedCallsThisMonth().executeAsOne().toInt()
        }

    override suspend fun blockedStats(): BlockedStats =
        withContext(dispatcher) {
            BlockedStats(
                today = queries.countBlockedCallsToday().executeAsOne().toInt(),
                thisWeek = queries.countBlockedCallsThisWeek().executeAsOne().toInt(),
                thisMonth = queries.countBlockedCallsThisMonth().executeAsOne().toInt(),
                pendingReview = queries.countPendingReview().executeAsOne().toInt(),
            )
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
                    rule_detail = decision.blockReason,
                ).value
        }
    }

    override suspend fun clearAll() {
        withContext(dispatcher) {
            queries.clearCallLog().value
        }
    }

    /**
     * Blocked-call counts for the last [daysBack] calendar days, newest first.
     *
     * Buckets align to UTC midnight — the same boundary [countBlockedCallsToday] uses — so
     * the first bucket's count always equals [BlockedStats.today]. Anchoring them to `now`
     * instead would make the newest bucket a rolling 24h window whose label ("Yesterday",
     * derived from its start) contradicted the calls inside it.
     */
    override suspend fun blockedByDay(daysBack: Int): List<DayStat> =
        withContext(dispatcher) {
            val entries = queries.selectAllCallLogEntries().executeAsList()
            val dayMillis = 86_400_000L
            val todayStart = (currentTimeMillis() / dayMillis) * dayMillis
            (0 until daysBack).map { dayOffset ->
                val dayStart = todayStart - dayOffset * dayMillis
                val dayEnd = dayStart + dayMillis
                val count =
                    entries.count {
                        it.action == "BLOCKED" && it.timestamp >= dayStart && it.timestamp < dayEnd
                    }
                DayStat(dateLabel = dayLabel(dayStart / dayMillis), count = count, cutoffEpochMillis = dayStart)
            }
        }

    private fun dayLabel(epochDay: Long): String {
        val now = currentTimeMillis()
        val today = now / 86_400_000L
        val diff = today - epochDay
        return when (diff) {
            0L -> "Today"
            1L -> "Yesterday"
            else -> "${diff}d ago"
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
