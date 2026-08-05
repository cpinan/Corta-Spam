package org.carlospinan.bloqueador.app.autoresponder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.testing.createTestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The real [SqlAutoResponderRepository], against a real SQLite engine.
 *
 * Like [org.carlospinan.bloqueador.app.settings.SqlSettingsRepository] it hydrates once at
 * construction, so "survives a restart" means: build a second repository over the same
 * database and read it back.
 */
class SqlAutoResponderRepositoryTest {
    @Test
    fun defaultsOnEmptyDatabase() =
        runTest {
            val config = SqlAutoResponderRepository(createTestDatabase(), Dispatchers.Unconfined).config.first()

            assertFalse(config.enabled)
            assertEquals(AutoResponderConfig.DEFAULT_SCRIPT, config.script)
            assertEquals("", config.audioUri)
            assertFalse(config.recordingEnabled)
            assertTrue(config.usesTts)
        }

    @Test
    fun settersUpdateTheExposedConfig() =
        runTest {
            val repo = SqlAutoResponderRepository(createTestDatabase(), Dispatchers.Unconfined)

            repo.setEnabled(true)
            repo.setScript("Please text instead.")
            repo.setAudioUri("content://greeting.m4a")
            repo.setRecordingEnabled(true)

            val config = repo.config.first()
            assertTrue(config.enabled)
            assertEquals("Please text instead.", config.script)
            assertEquals("content://greeting.m4a", config.audioUri)
            assertTrue(config.recordingEnabled)
            // An audio URI takes over from text-to-speech.
            assertFalse(config.usesTts)
        }

    @Test
    fun everyFieldSurvivesARestart() =
        runTest {
            val db = createTestDatabase()
            val first = SqlAutoResponderRepository(db, Dispatchers.Unconfined)

            first.setEnabled(true)
            first.setScript("Please text instead.")
            first.setAudioUri("content://greeting.m4a")
            first.setRecordingEnabled(true)

            val config = SqlAutoResponderRepository(db, Dispatchers.Unconfined).config.first()
            assertTrue(config.enabled)
            assertEquals("Please text instead.", config.script)
            assertEquals("content://greeting.m4a", config.audioUri)
            assertTrue(config.recordingEnabled)
        }

    @Test
    fun anEmptiedScriptRevertsToTheDefaultOnRestart() =
        runTest {
            val db = createTestDatabase()
            val repo = SqlAutoResponderRepository(db, Dispatchers.Unconfined)
            repo.setScript("Please text instead.")

            repo.setScript("")

            // In-memory the field really is empty...
            assertEquals("", repo.config.first().script)
            // ...but KeyValueSettingsStore.readString treats a stored blank as "unset", so a
            // restart resurrects the default script rather than an empty one. Clearing the
            // script in the UI therefore does not persist as "no script".
            assertEquals(AutoResponderConfig.DEFAULT_SCRIPT, SqlAutoResponderRepository(db, Dispatchers.Unconfined).config.first().script)
        }

    @Test
    fun aClearedAudioUriStaysClearedAcrossARestart() =
        runTest {
            val db = createTestDatabase()
            val repo = SqlAutoResponderRepository(db, Dispatchers.Unconfined)
            repo.setAudioUri("content://greeting.m4a")

            repo.setAudioUri("")

            // Here the blank-is-unset behaviour is harmless: the default is "" too.
            assertEquals("", SqlAutoResponderRepository(db, Dispatchers.Unconfined).config.first().audioUri)
        }
}
