package org.carlospinan.bloqueador.app.rules

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.carlospinan.bloqueador.app.calllog.RecordingStore
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
    ): Long =
        withContext(dispatcher) {
            // The insert and the id read share one transaction: last_insert_rowid() is
            // connection-scoped, so a concurrent insert between the two -- two calls arriving
            // together, which call waiting makes real -- would otherwise hand back the wrong row.
            queries.transactionWithResult {
                queries
                    .insertCallLogEntry(
                        number = number,
                        timestamp = timestamp,
                        action = if (decision.isBlocked) "BLOCKED" else "ALLOWED",
                        rule_type = decision.ruleTypeTag,
                        rule_id = decision.loggedRuleId,
                        rule_detail = decision.loggedDetail,
                        direction = CallDirection.INCOMING.name,
                    ).value
                queries.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun logOutgoingCall(
        number: String,
        timestamp: Long,
    ): Long =
        withContext(dispatcher) {
            // ALLOWED with no rule tag: the call was never screened, and "BLOCKED" is the only
            // other value the CHECK constraint permits. Stats count blocked rows, so an outgoing
            // row can never inflate the blocked-calls counters.
            queries.transactionWithResult {
                queries
                    .insertCallLogEntry(
                        number = number,
                        timestamp = timestamp,
                        action = "ALLOWED",
                        rule_type = null,
                        rule_id = null,
                        rule_detail = null,
                        direction = CallDirection.OUTGOING.name,
                    ).value
                queries.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun attachRecording(
        entryId: Long,
        path: String,
    ) {
        withContext(dispatcher) {
            queries.updateCallLogRecordingPath(recording_path = path, id = entryId).value
        }
    }

    override suspend fun deleteRecording(entryId: Long) {
        withContext(dispatcher) {
            // executeAsOneOrNull() collapses "no such row" and "row whose path is null" into the
            // same null, which is fine -- both mean there is nothing to delete.
            val path = queries.selectRecordingPathById(entryId).executeAsOneOrNull()?.recording_path
            if (path != null) RecordingStore.delete(path)
            // Clear the column even when the file delete failed. A row pointing at a file that
            // is still on disk is worse than an orphan: the UI would keep offering playback of
            // a recording the user asked to destroy.
            queries.clearCallLogRecordingPath(entryId).value
        }
    }

    override suspend fun clearAll() {
        withContext(dispatcher) {
            // Files first: once clearCallLog runs, the paths are gone and the audio is
            // unreachable but still on disk. "Clear log" that leaves recorded callers behind is
            // exactly the promise this app's privacy policy makes and would be breaking.
            // SQLDelight narrows this to Query<String> off the IS NOT NULL predicate, so no
            // null check is needed here -- unlike selectRecordingPathById above, which has no
            // such predicate and comes back wrapped with a nullable column.
            queries.selectAllRecordingPaths().executeAsList().forEach { RecordingStore.delete(it) }
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
            recordingPath = recording_path,
            // A row written by a future version with a direction this build has never heard of
            // reads as INCOMING rather than crashing the whole call log on one bad string.
            direction = CallDirection.entries.firstOrNull { it.name == direction } ?: CallDirection.INCOMING,
        )
}
