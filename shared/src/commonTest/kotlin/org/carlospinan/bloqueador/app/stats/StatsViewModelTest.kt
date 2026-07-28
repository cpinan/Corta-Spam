package org.carlospinan.bloqueador.app.stats

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.carlospinan.bloqueador.app.rules.BlockedStats
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.DayStat
import org.carlospinan.bloqueador.app.rules.RuleDecision
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private class FakeCallLogRepository(
        var dailyStats: List<DayStat> = emptyList(),
    ) : CallLogRepository {
        override fun allEntries() = MutableStateFlow(emptyList<CallLogEntryData>())
        override fun recentEntries(limit: Int) = MutableStateFlow(emptyList<CallLogEntryData>())
        override suspend fun blockedCountToday(): Int = 0
        override suspend fun blockedCountThisWeek(): Int = 0
        override suspend fun blockedCountThisMonth(): Int = 0
        override suspend fun blockedStats(): BlockedStats = BlockedStats(0, 0, 0)
        override suspend fun blockedByDay(daysBack: Int): List<DayStat> = dailyStats
        override suspend fun logCall(number: String, timestamp: Long, decision: RuleDecision) {}
        override suspend fun clearAll() {}
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads stats and sets isLoading false`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo =
            FakeCallLogRepository(
                dailyStats =
                    listOf(
                        DayStat("Mon", 5, 1000L),
                        DayStat("Tue", 3, 2000L),
                    ),
                )
        val vm = StatsViewModel(repo)
        advanceUntilIdle()

        val state = vm.state.first { !it.isLoading }
        assertEquals(2, state.dailyStats.size)
        assertEquals("Mon", state.dailyStats[0].dateLabel)
        assertEquals(5, state.dailyStats[0].count)
    }

    @Test
    fun `initial state is loading`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeCallLogRepository()
        val vm = StatsViewModel(repo)
        assertTrue(vm.state.value.isLoading)
    }

    @Test
    fun `empty daily stats not loading`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeCallLogRepository()
        val vm = StatsViewModel(repo)
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
        assertTrue(vm.state.value.dailyStats.isEmpty())
    }
}
