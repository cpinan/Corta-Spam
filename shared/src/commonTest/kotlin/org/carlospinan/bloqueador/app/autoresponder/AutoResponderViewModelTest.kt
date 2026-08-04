package org.carlospinan.bloqueador.app.autoresponder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.carlospinan.bloqueador.app.testing.FakeAutoResponderRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [AutoResponderViewModel] is a thin mapper, but the thing it maps matters: it runs
 * [AutoResponderConfig.validate] on every emission and surfaces the error code to the UI.
 * These tests pin which config shapes produce which code — `MISSING_CONSENT` in particular,
 * since call recording without a consent line in the greeting is a legal problem, not a
 * cosmetic one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoResponderViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repositoryWith(config: AutoResponderConfig) = FakeAutoResponderRepository(MutableStateFlow(config))

    @Test
    fun `state exposes the repository config`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = repositoryWith(AutoResponderConfig(enabled = true, script = "Hi there."))
            val vm = AutoResponderViewModel(repo)

            val state = vm.state.first { it.config.enabled }
            assertEquals("Hi there.", state.config.script)
            assertNull(state.validationError)
        }

    @Test
    fun `a disabled responder never reports a validation error`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            // Blank script and blank audio would be invalid if enabled.
            val repo = repositoryWith(AutoResponderConfig(enabled = false, script = "", audioUri = ""))
            val vm = AutoResponderViewModel(repo)
            advanceUntilIdle()

            assertNull(vm.state.value.validationError)
        }

    @Test
    fun `enabled with no script and no audio reports EMPTY_SCRIPT`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = repositoryWith(AutoResponderConfig(enabled = true, script = "  ", audioUri = ""))
            val vm = AutoResponderViewModel(repo)

            val state = vm.state.first { it.validationError != null }
            assertEquals(AutoResponderConfig.ErrorCode.EMPTY_SCRIPT, state.validationError)
        }

    @Test
    fun `an audio greeting satisfies the empty-script rule`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = repositoryWith(AutoResponderConfig(enabled = true, script = "", audioUri = "content://clip.m4a"))
            val vm = AutoResponderViewModel(repo)

            val state = vm.state.first { it.config.enabled }
            assertNull(state.validationError)
            assertFalse(state.config.usesTts)
        }

    @Test
    fun `a script over the length cap reports SCRIPT_TOO_LONG`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val tooLong = "a".repeat(AutoResponderConfig.MAX_SCRIPT_LENGTH + 1)
            val repo = repositoryWith(AutoResponderConfig(enabled = true, script = tooLong))
            val vm = AutoResponderViewModel(repo)

            val state = vm.state.first { it.validationError != null }
            assertEquals(AutoResponderConfig.ErrorCode.SCRIPT_TOO_LONG, state.validationError)
        }

    @Test
    fun `recording without the consent marker reports MISSING_CONSENT`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo =
                repositoryWith(
                    AutoResponderConfig(enabled = true, script = "Leave a message.", recordingEnabled = true),
                )
            val vm = AutoResponderViewModel(repo)

            val state = vm.state.first { it.validationError != null }
            assertEquals(AutoResponderConfig.ErrorCode.MISSING_CONSENT, state.validationError)
        }

    @Test
    fun `recording with the consent marker validates`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo =
                repositoryWith(
                    AutoResponderConfig(
                        enabled = true,
                        script = "${AutoResponderConfig.CONSENT_MARKER}. Leave a message.",
                        recordingEnabled = true,
                    ),
                )
            val vm = AutoResponderViewModel(repo)

            val state = vm.state.first { it.config.recordingEnabled }
            assertNull(state.validationError)
        }

    @Test
    fun `validation error clears once the config is fixed`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = repositoryWith(AutoResponderConfig(enabled = true, script = ""))
            val vm = AutoResponderViewModel(repo)
            assertEquals(
                AutoResponderConfig.ErrorCode.EMPTY_SCRIPT,
                vm.state.first { it.validationError != null }.validationError,
            )

            vm.onIntent(AutoResponderIntent.SetScript("Now it has a script."))
            advanceUntilIdle()

            assertNull(vm.state.first { it.validationError == null }.validationError)
        }

    @Test
    fun `SetEnabled reaches the repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeAutoResponderRepository()
            val vm = AutoResponderViewModel(repo)

            vm.onIntent(AutoResponderIntent.SetEnabled(true))
            advanceUntilIdle()

            assertTrue(repo.config.value.enabled)
        }

    @Test
    fun `SetAudioUri and SetRecordingEnabled reach the repository`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeAutoResponderRepository()
            val vm = AutoResponderViewModel(repo)

            vm.onIntent(AutoResponderIntent.SetAudioUri("content://clip.m4a"))
            vm.onIntent(AutoResponderIntent.SetRecordingEnabled(true))
            advanceUntilIdle()

            assertEquals("content://clip.m4a", repo.config.value.audioUri)
            assertTrue(repo.config.value.recordingEnabled)
        }

    @Test
    fun `ClearAudioUri blanks the uri and falls back to text-to-speech`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = repositoryWith(AutoResponderConfig(audioUri = "content://clip.m4a"))
            val vm = AutoResponderViewModel(repo)

            vm.onIntent(AutoResponderIntent.ClearAudioUri)
            advanceUntilIdle()

            assertEquals("", repo.config.value.audioUri)
            assertTrue(repo.config.value.usesTts)
        }
}
