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
            // Exported carries the JSON for the platform share sheet; the second effect is the
            // toast signal. It carries no text -- the wording is picked in the Composable, in
            // the reader's locale, rather than being built in English here.
            assertEquals(BackupEffect.Exported("""{"blockedNumbers":[]}"""), emitted[0])
            assertEquals(BackupEffect.ExportSucceeded, emitted[1])
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

            assertEquals(BackupEffect.ExportFailed("disk full"), effects.await().single())
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
            // The counts travel as data; BackupScreen turns them into a localized sentence.
            val counts = (effects.await().single() as BackupEffect.ImportSucceeded).result
            assertEquals(11, counts.total)
            assertEquals(2, counts.blockedNumbersImported)
            assertEquals(4, counts.schedulesImported)
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

            assertEquals(0, (effects.await().single() as BackupEffect.ImportSucceeded).result.total)
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

            assertEquals(BackupEffect.ImportFailed("malformed JSON"), effects.await().single())
        }
}
