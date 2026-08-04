package org.carlospinan.bloqueador.app.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.carlospinan.bloqueador.app.testing.FakeRuleRepository
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [BackupViewModel] has no UiState at all — everything it produces is a one-time
 * [BackupEffect] on a rendezvous [kotlinx.coroutines.channels.Channel]. That shape is the
 * reason each test starts a collector *before* dispatching an intent: with no subscriber,
 * `send` suspends and the effect is never observed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Export emits the payload and then a success message`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository().apply { exportJson = """{"blockedNumbers":[]}""" }
            val vm = BackupViewModel(repo)
            val effects = async { vm.effect.take(2).toList() }
            advanceUntilIdle()

            vm.onIntent(BackupIntent.Export)
            advanceUntilIdle()

            val emitted = effects.await()
            // Exported carries the JSON for the platform share sheet; Success is the toast.
            assertEquals(BackupEffect.Exported("""{"blockedNumbers":[]}"""), emitted[0])
            assertEquals(BackupEffect.Success("Exported successfully."), emitted[1])
        }

    @Test
    fun `a failing export reports the failure instead of the payload`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository().apply { backupFailure = IllegalStateException("disk full") }
            val vm = BackupViewModel(repo)
            val effects = async { vm.effect.take(1).toList() }
            advanceUntilIdle()

            vm.onIntent(BackupIntent.Export)
            advanceUntilIdle()

            assertEquals(BackupEffect.Failure("Export failed: disk full"), effects.await().single())
        }

    @Test
    fun `Import hands the json to the repository and summarises the counts`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo =
                FakeRuleRepository().apply {
                    importResult =
                        ImportResult(
                            blockedNumbersImported = 2,
                            allowlistedNumbersImported = 1,
                            patternsImported = 3,
                            countriesImported = 1,
                            actionsImported = 0,
                            schedulesImported = 4,
                        )
                }
            val vm = BackupViewModel(repo)
            val effects = async { vm.effect.take(1).toList() }
            advanceUntilIdle()

            vm.onIntent(BackupIntent.Import("""{"blockedNumbers":[]}"""))
            advanceUntilIdle()

            assertEquals(listOf("""{"blockedNumbers":[]}"""), repo.importedJson)
            val message = (effects.await().single() as BackupEffect.Success).message
            assertTrue(message.startsWith("Imported 11 rules"), message)
            assertTrue(message.contains("2 blocked"), message)
            assertTrue(message.contains("4 schedules"), message)
        }

    @Test
    fun `an empty import still reports success with a zero total`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val vm = BackupViewModel(FakeRuleRepository())
            val effects = async { vm.effect.take(1).toList() }
            advanceUntilIdle()

            vm.onIntent(BackupIntent.Import("{}"))
            advanceUntilIdle()

            val message = (effects.await().single() as BackupEffect.Success).message
            assertTrue(message.startsWith("Imported 0 rules"), message)
        }

    @Test
    fun `a failing import reports the failure`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repo = FakeRuleRepository().apply { backupFailure = IllegalArgumentException("malformed JSON") }
            val vm = BackupViewModel(repo)
            val effects = async { vm.effect.take(1).toList() }
            advanceUntilIdle()

            vm.onIntent(BackupIntent.Import("not json"))
            advanceUntilIdle()

            assertEquals(BackupEffect.Failure("Import failed: malformed JSON"), effects.await().single())
        }
}
