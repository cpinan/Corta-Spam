package org.carlospinan.bloqueador.app.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.carlospinan.bloqueador.app.testing.FakeSettingsRepository
import org.carlospinan.bloqueador.app.testing.FakeSpamProviderRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [SettingsViewModel] merges six flows from two repositories. Five go through the typed
 * `combine()` overload; the sixth is chained on top because kotlinx.coroutines' typed
 * overloads stop at five. That chaining is easy to break when a setting is added, so the
 * sixth field gets its own test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        spam: FakeSpamProviderRepository = FakeSpamProviderRepository(),
    ) = SettingsViewModel(settings, spam)

    @Test
    fun `state starts at the UiState defaults`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = viewModel()

            // stateIn's initial value, before any repository flow has been collected.
            assertEquals(SettingsUiState(), vm.state.value)
        }

    @Test
    fun `state reflects every repository flow`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings =
                FakeSettingsRepository(
                    blockingEnabled = MutableStateFlow(false),
                    autoAllowContacts = MutableStateFlow(false),
                    defaultAction = MutableStateFlow(DefaultAction.BLOCK),
                    notificationsEnabled = MutableStateFlow(false),
                    repeatedCallerBypassCount = MutableStateFlow(4),
                )
            val vm = viewModel(settings, FakeSpamProviderRepository(MutableStateFlow(true)))

            val state = vm.state.first { !it.blockingEnabled }
            assertFalse(state.blockingEnabled)
            assertFalse(state.autoAllowContacts)
            assertEquals(DefaultAction.BLOCK, state.defaultAction)
            assertFalse(state.notificationsEnabled)
            assertEquals(4, state.repeatedCallerBypassCount)
            assertTrue(state.spamEnabled)
        }

    @Test
    fun `repeatedCallerBypassCount is threaded through the chained combine`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings = FakeSettingsRepository()
            val vm = viewModel(settings)
            advanceUntilIdle()

            settings.repeatedCallerBypassCount.value = 7

            // The sixth flow rides a second combine() stacked on the first. If a future
            // setting is added by widening the typed combine instead of chaining again,
            // this is the assertion that catches the dropped field.
            assertEquals(7, vm.state.first { it.repeatedCallerBypassCount == 7 }.repeatedCallerBypassCount)
        }

    // No comma in the name: Kotlin/Native rejects "," in a backticked identifier, so a
    // comma here compiles on Android and fails the iOS test compile.
    @Test
    fun `spamEnabled comes from the spam repository and not from settings`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val spam = FakeSpamProviderRepository()
            val vm = viewModel(spam = spam)
            advanceUntilIdle()

            spam.enabled.value = true

            assertTrue(vm.state.first { it.spamEnabled }.spamEnabled)
        }

    @Test
    fun `SetBlockingEnabled reaches the settings repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings = FakeSettingsRepository()
            val vm = viewModel(settings)

            vm.onIntent(SettingsIntent.SetBlockingEnabled(false))
            advanceUntilIdle()

            assertFalse(settings.blockingEnabled.value)
        }

    @Test
    fun `SetAutoAllowContacts reaches the settings repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings = FakeSettingsRepository()
            val vm = viewModel(settings)

            vm.onIntent(SettingsIntent.SetAutoAllowContacts(true))
            advanceUntilIdle()

            assertTrue(settings.autoAllowContacts.value)
        }

    @Test
    fun `SetDefaultAction reaches the settings repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings = FakeSettingsRepository()
            val vm = viewModel(settings)

            vm.onIntent(SettingsIntent.SetDefaultAction(DefaultAction.ASK))
            advanceUntilIdle()

            assertEquals(DefaultAction.ASK, settings.defaultAction.value)
        }

    @Test
    fun `SetSpamEnabled reaches the spam repository only`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings = FakeSettingsRepository()
            val spam = FakeSpamProviderRepository()
            val vm = viewModel(settings, spam)

            vm.onIntent(SettingsIntent.SetSpamEnabled(true))
            advanceUntilIdle()

            assertTrue(spam.enabled.value)
            // The spam toggle must not touch blocking; they are separate repositories.
            assertTrue(settings.blockingEnabled.value)
        }

    @Test
    fun `SetNotificationsEnabled reaches the settings repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings = FakeSettingsRepository()
            val vm = viewModel(settings)

            vm.onIntent(SettingsIntent.SetNotificationsEnabled(false))
            advanceUntilIdle()

            assertFalse(settings.notificationsEnabled.value)
        }

    @Test
    fun `SetRepeatedCallerBypassCount reaches the settings repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val settings = FakeSettingsRepository()
            val vm = viewModel(settings)

            vm.onIntent(SettingsIntent.SetRepeatedCallerBypassCount(5))
            advanceUntilIdle()

            assertEquals(5, settings.repeatedCallerBypassCount.value)
        }
}
