package org.carlospinan.bloqueador.app.calllog

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.rules.BlockedStats
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.DayStat
import org.carlospinan.bloqueador.app.rules.RuleDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CallLogViewModelTest {

    private class FakeCallLogRepository(
        private val entriesFlow: MutableStateFlow<List<CallLogEntryData>> =
            MutableStateFlow(emptyList()),
    ) : CallLogRepository {
        override fun allEntries() = entriesFlow
        override fun recentEntries(limit: Int) = entriesFlow
        override suspend fun blockedCountToday(): Int = 0
        override suspend fun blockedCountThisWeek(): Int = 0
        override suspend fun blockedCountThisMonth(): Int = 0
        override suspend fun blockedStats(): BlockedStats = BlockedStats(0, 0, 0)
        override suspend fun blockedByDay(daysBack: Int): List<DayStat> = emptyList()
        override suspend fun logCall(number: String, timestamp: Long, decision: RuleDecision) {}
        override suspend fun clearAll() {}
    }

    @Test
    fun `entries flow reflects repository data`() = runTest {
        val entry =
            CallLogEntryData(
                id = 1L,
                number = "+34611223344",
                timestamp = 1234567890L,
                action = "BLOCKED",
                ruleType = "MANUAL",
                ruleId = 1L,
                ruleDetail = "Manually blocked",
            )
        val flow = MutableStateFlow(listOf(entry))
        val repo = FakeCallLogRepository(entriesFlow = flow)
        val vm = CallLogViewModel(callLogRepository = repo)

        val entries = vm.entries.first { it.isNotEmpty() }
        assertEquals(1, entries.size)
        assertEquals("+34611223344", entries[0].number)
        assertEquals("BLOCKED", entries[0].action)
        assertEquals("Manually blocked", entries[0].ruleDetail)
    }

    @Test
    fun `entries starts empty`() = runTest {
        val repo = FakeCallLogRepository()
        val vm = CallLogViewModel(callLogRepository = repo)

        val entries = vm.entries.first()
        assertTrue(entries.isEmpty())
    }
}
