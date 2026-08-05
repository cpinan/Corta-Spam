package org.carlospinan.bloqueador.app.spam

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.testing.createTestDatabase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The real [SqlSpamProviderRepository], against a real SQLite engine.
 *
 * One flag, but a privacy-relevant one: it gates whether numbers are looked up against an
 * external provider, so its default must be off and must stay off unless explicitly enabled.
 */
class SqlSpamProviderRepositoryTest {
    @Test
    fun defaultsToDisabled() =
        runTest {
            assertFalse(SqlSpamProviderRepository(createTestDatabase(), Dispatchers.Unconfined).enabled.first())
        }

    @Test
    fun enablingUpdatesTheExposedFlow() =
        runTest {
            val repo = SqlSpamProviderRepository(createTestDatabase(), Dispatchers.Unconfined)

            repo.setEnabled(true)

            assertTrue(repo.enabled.first())
        }

    @Test
    fun enabledSurvivesARestart() =
        runTest {
            val db = createTestDatabase()
            SqlSpamProviderRepository(db, Dispatchers.Unconfined).setEnabled(true)

            assertTrue(SqlSpamProviderRepository(db, Dispatchers.Unconfined).enabled.first())
        }

    @Test
    fun disablingSurvivesARestart() =
        runTest {
            val db = createTestDatabase()
            SqlSpamProviderRepository(db, Dispatchers.Unconfined).setEnabled(true)

            SqlSpamProviderRepository(db, Dispatchers.Unconfined).setEnabled(false)

            assertFalse(SqlSpamProviderRepository(db, Dispatchers.Unconfined).enabled.first())
        }
}
