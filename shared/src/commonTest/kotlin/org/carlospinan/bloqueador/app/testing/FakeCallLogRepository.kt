package org.carlospinan.bloqueador.app.testing

import kotlinx.coroutines.flow.MutableStateFlow
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

    override suspend fun blockedCountToday(): Int = stats.today

    override suspend fun blockedCountThisWeek(): Int = stats.thisWeek

    override suspend fun blockedCountThisMonth(): Int = stats.thisMonth

    override suspend fun blockedStats(): BlockedStats = stats

    override suspend fun blockedByDay(daysBack: Int): List<DayStat> = dailyStats

    override suspend fun logCall(
        number: String,
        timestamp: Long,
        decision: RuleDecision,
    ) {}

    override suspend fun clearAll() {}
}
