package org.carlospinan.bloqueador.app.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.db.KeyValueSettingsStore
import org.carlospinan.bloqueador.app.testing.createTestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The real [SqlSettingsRepository], against a real SQLite engine.
 *
 * Its distinguishing behaviour is init-time hydration: flows are seeded from the database
 * once, in `init`, and never re-read. So "does a setting survive an app restart" is only
 * answerable by constructing a *second* repository over the same database — which is what
 * most tests here do.
 */
class SqlSettingsRepositoryTest {
    @Test
    fun defaultsOnEmptyDatabase() =
        runTest {
            val repo = SqlSettingsRepository(createTestDatabase())

            assertTrue(repo.blockingEnabled.first())
            assertTrue(repo.autoAllowContacts.first())
            assertEquals(DefaultAction.ALLOW, repo.defaultAction.first())
            assertTrue(repo.notificationsEnabled.value)
            assertEquals(0, repo.repeatedCallerBypassCount.first())
            assertFalse(repo.welcomeShown)
        }

    @Test
    fun settersUpdateTheExposedFlows() =
        runTest {
            val repo = SqlSettingsRepository(createTestDatabase())

            repo.setBlockingEnabled(false)
            repo.setAutoAllowContacts(false)
            repo.setDefaultAction(DefaultAction.BLOCK)
            repo.setNotificationsEnabled(false)
            repo.setRepeatedCallerBypassCount(4)

            assertFalse(repo.blockingEnabled.first())
            assertFalse(repo.autoAllowContacts.first())
            assertEquals(DefaultAction.BLOCK, repo.defaultAction.first())
            assertFalse(repo.notificationsEnabled.value)
            assertEquals(4, repo.repeatedCallerBypassCount.first())
        }

    @Test
    fun everySettingSurvivesARestart() =
        runTest {
            val db = createTestDatabase()
            val first = SqlSettingsRepository(db)

            first.setBlockingEnabled(false)
            first.setAutoAllowContacts(false)
            first.setDefaultAction(DefaultAction.ASK)
            first.setNotificationsEnabled(false)
            first.setRepeatedCallerBypassCount(9)

            val restarted = SqlSettingsRepository(db)

            assertFalse(restarted.blockingEnabled.first())
            assertFalse(restarted.autoAllowContacts.first())
            assertEquals(DefaultAction.ASK, restarted.defaultAction.first())
            assertFalse(restarted.notificationsEnabled.value)
            assertEquals(9, restarted.repeatedCallerBypassCount.first())
        }

    @Test
    fun welcomeShownIsFalseUntilSetAndThenSurvivesARestart() =
        runTest {
            val db = createTestDatabase()
            val first = SqlSettingsRepository(db)
            assertFalse(first.welcomeShown)

            first.setWelcomeShown()

            assertTrue(first.welcomeShown)
            assertTrue(SqlSettingsRepository(db).welcomeShown)
        }

    @Test
    fun unknownStoredDefaultActionFallsBackToAllow() =
        runTest {
            val db = createTestDatabase()
            // Simulates a value written by a newer build, or a corrupted row.
            KeyValueSettingsStore(db).write("default_action", "TRANSFER_TO_VOICEMAIL")

            assertEquals(DefaultAction.ALLOW, SqlSettingsRepository(db).defaultAction.first())
        }

    @Test
    fun everyDefaultActionKeyRoundTrips() =
        runTest {
            DefaultAction.entries.forEach { action ->
                val db = createTestDatabase()
                SqlSettingsRepository(db).setDefaultAction(action)

                assertEquals(action, SqlSettingsRepository(db).defaultAction.first())
            }
        }

    @Test
    fun aLiveRepositoryDoesNotSeeAnotherInstancesWrite() =
        runTest {
            val db = createTestDatabase()
            val observer = SqlSettingsRepository(db)
            val writer = SqlSettingsRepository(db)

            writer.setBlockingEnabled(false)

            // Hydration happens once in init; the flows are not backed by a database query,
            // so a second instance's write is invisible until the observer is rebuilt. Safe
            // in the app only because SharedModule binds it as `single<SettingsRepository>`
            // — two live instances would silently disagree.
            assertTrue(observer.blockingEnabled.first())
            assertFalse(SqlSettingsRepository(db).blockingEnabled.first())
        }
}
