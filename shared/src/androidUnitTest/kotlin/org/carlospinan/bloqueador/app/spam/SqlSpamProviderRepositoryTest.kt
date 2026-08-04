package org.carlospinan.bloqueador.app.spam

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
            assertFalse(SqlSpamProviderRepository(createTestDatabase()).enabled.first())
        }

    @Test
    fun enablingUpdatesTheExposedFlow() =
        runTest {
            val repo = SqlSpamProviderRepository(createTestDatabase())

            repo.setEnabled(true)

            assertTrue(repo.enabled.first())
        }

    @Test
    fun enabledSurvivesARestart() =
        runTest {
            val db = createTestDatabase()
            SqlSpamProviderRepository(db).setEnabled(true)

            assertTrue(SqlSpamProviderRepository(db).enabled.first())
        }

    @Test
    fun disablingSurvivesARestart() =
        runTest {
            val db = createTestDatabase()
            SqlSpamProviderRepository(db).setEnabled(true)

            SqlSpamProviderRepository(db).setEnabled(false)

            assertFalse(SqlSpamProviderRepository(db).enabled.first())
        }
}
