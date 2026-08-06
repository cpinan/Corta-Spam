package org.carlospinan.bloqueador.app.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.carlospinan.bloqueador.app.rules.BlockedStats
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.DayStat
import org.carlospinan.bloqueador.app.rules.RuleDecision

/**
 * In-memory [CallLogRepository] for tests.
 *
 * [stats] and [dailyStats] are `var` so a test can change them mid-run and then
 * trigger a refresh. [recentEntries] ignores its `limit` and returns [entriesFlow]
 * whole; no test exercises truncation.
 */
internal class FakeCallLogRepository(
    val entriesFlow: MutableStateFlow<List<CallLogEntryData>> = MutableStateFlow(emptyList()),
    var stats: BlockedStats = BlockedStats(0, 0, 0),
    var dailyStats: List<DayStat> = emptyList(),
) : CallLogRepository {
    override fun allEntries() = entriesFlow

    override fun recentEntries(limit: Int) = entriesFlow

    /**
     * Mirrors the real implementation's contract: emits once immediately, then again on every
     * call-log change. Driven off [entriesFlow] so a test can trigger a Home refresh by pushing
     * a new list into it.
     */
    override fun changes(): Flow<Unit> = entriesFlow.map { }

    override suspend fun blockedCountToday(): Int = stats.today

    override suspend fun blockedCountThisWeek(): Int = stats.thisWeek

    override suspend fun blockedCountThisMonth(): Int = stats.thisMonth

    override suspend fun blockedStats(): BlockedStats = stats

    override suspend fun blockedByDay(daysBack: Int): List<DayStat> = dailyStats

    /** Ids handed out by [logCall], in order, so a test can assert what the caller was given. */
    val loggedIds = mutableListOf<Long>()

    /** Recordings attached via [attachRecording], keyed by entry id. */
    val attachedRecordings = mutableMapOf<Long, String>()

    private var nextId = 1L

    override suspend fun logCall(
        number: String,
        timestamp: Long,
        decision: RuleDecision,
    ): Long = nextId++.also { loggedIds += it }

    override suspend fun attachRecording(
        entryId: Long,
        path: String,
    ) {
        attachedRecordings[entryId] = path
    }

    override suspend fun deleteRecording(entryId: Long) {
        attachedRecordings.remove(entryId)
    }

    override suspend fun clearAll() {
        attachedRecordings.clear()
        entriesFlow.value = emptyList()
    }
}
