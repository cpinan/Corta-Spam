package org.carlospinan.bloqueador.app.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.carlospinan.bloqueador.app.rules.BlockedStats
import org.carlospinan.bloqueador.app.testing.FakeCallLogRepository
import org.carlospinan.bloqueador.app.testing.FakeSettingsRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads blocked stats from repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeCallLogRepository(stats = BlockedStats(5, 12, 30))
            val settings = FakeSettingsRepository()
            val vm = HomeViewModel(callLogRepository = repo, settingsRepository = settings)
            advanceUntilIdle()

            val state = vm.state.first { !it.isLoading }
            assertEquals(5, state.blockedToday)
            assertEquals(12, state.blockedThisWeek)
            assertEquals(30, state.blockedThisMonth)
        }

    @Test
    fun `refresh updates stats`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeCallLogRepository(stats = BlockedStats(1, 2, 3))
            val settings = FakeSettingsRepository()
            val vm = HomeViewModel(callLogRepository = repo, settingsRepository = settings)
            advanceUntilIdle()

            repo.stats = BlockedStats(10, 20, 30)
            vm.onIntent(HomeIntent.Refresh)
            advanceUntilIdle()

            val state = vm.state.first { it.blockedToday == 10 }
            assertEquals(10, state.blockedToday)
            assertEquals(20, state.blockedThisWeek)
            assertEquals(30, state.blockedThisMonth)
        }

    @Test
    fun `initial state is loading`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeCallLogRepository()
            val settings = FakeSettingsRepository()
            val vm = HomeViewModel(callLogRepository = repo, settingsRepository = settings)

            val first = vm.state.value
            assertTrue(first.isLoading)
        }

    @Test
    fun `toggle blocking updates repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeCallLogRepository()
            val settings = FakeSettingsRepository()
            val vm = HomeViewModel(callLogRepository = repo, settingsRepository = settings)

            vm.onIntent(HomeIntent.ToggleBlocking(false))
            advanceUntilIdle()
            val enabled = settings.blockingEnabled.first()
            assertFalse(enabled)
        }

    @Test
    fun `blocking enabled defaults to true`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeCallLogRepository()
            val settings = FakeSettingsRepository()
            val vm = HomeViewModel(callLogRepository = repo, settingsRepository = settings)
            advanceUntilIdle()

            assertTrue(vm.state.value.blockingEnabled)
        }
}
