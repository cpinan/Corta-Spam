package org.carlospinan.bloqueador.app.autoresponder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.carlospinan.bloqueador.app.db.AppDatabase

class SqlAutoResponderRepository(
    private val db: AppDatabase,
) : AutoResponderRepository {
    private val _config = MutableStateFlow(loadConfig())
    override val config: Flow<AutoResponderConfig> = _config.asStateFlow()

    private fun loadConfig(): AutoResponderConfig =
        AutoResponderConfig(
            enabled = readBool(KEY_ENABLED, false),
            script = readString(KEY_SCRIPT, AutoResponderConfig.DEFAULT_SCRIPT),
            audioUri = readString(KEY_AUDIO_URI, ""),
            recordingEnabled = readBool(KEY_RECORDING, false),
        )

    private fun readBool(
        key: String,
        default: Boolean,
    ): Boolean {
        val raw = db.appDatabaseQueries.selectSetting(key).executeAsOneOrNull() ?: return default
        return raw.toBooleanStrictOrNull() ?: default
    }

    private fun readString(
        key: String,
        default: String,
    ): String {
        val raw = db.appDatabaseQueries.selectSetting(key).executeAsOneOrNull() ?: return default
        return raw.ifBlank { default }
    }

    private suspend fun upsert(
        key: String,
        value: String,
    ) {
        withContext(Dispatchers.IO) {
            db.appDatabaseQueries.upsertSetting(key, value)
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        upsert(KEY_ENABLED, enabled.toString())
        _config.value = _config.value.copy(enabled = enabled)
    }

    override suspend fun setScript(script: String) {
        upsert(KEY_SCRIPT, script)
        _config.value = _config.value.copy(script = script)
    }

    override suspend fun setAudioUri(uri: String) {
        upsert(KEY_AUDIO_URI, uri)
        _config.value = _config.value.copy(audioUri = uri)
    }

    override suspend fun setRecordingEnabled(enabled: Boolean) {
        upsert(KEY_RECORDING, enabled.toString())
        _config.value = _config.value.copy(recordingEnabled = enabled)
    }

    companion object {
        private const val KEY_ENABLED = "auto_responder_enabled"
        private const val KEY_SCRIPT = "auto_responder_script"
        private const val KEY_AUDIO_URI = "auto_responder_audio_uri"
        private const val KEY_RECORDING = "auto_recording_enabled"
    }
}
