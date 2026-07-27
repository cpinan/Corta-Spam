package org.carlospinan.bloqueador.app.autoresponder

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.carlospinan.bloqueador.app.db.AppDatabase
import org.carlospinan.bloqueador.app.db.KeyValueSettingsStore

class SqlAutoResponderRepository(
    db: AppDatabase,
) : AutoResponderRepository {
    private val store = KeyValueSettingsStore(db)

    private val _config = MutableStateFlow(loadConfig())
    override val config: Flow<AutoResponderConfig> = _config.asStateFlow()

    private fun loadConfig(): AutoResponderConfig =
        AutoResponderConfig(
            enabled = store.readBool(KEY_ENABLED, false),
            script = store.readString(KEY_SCRIPT, AutoResponderConfig.DEFAULT_SCRIPT),
            audioUri = store.readString(KEY_AUDIO_URI, ""),
            recordingEnabled = store.readBool(KEY_RECORDING, false),
        )

    override suspend fun setEnabled(enabled: Boolean) {
        store.writeBool(KEY_ENABLED, enabled)
        _config.value = _config.value.copy(enabled = enabled)
    }

    override suspend fun setScript(script: String) {
        store.write(KEY_SCRIPT, script)
        _config.value = _config.value.copy(script = script)
    }

    override suspend fun setAudioUri(uri: String) {
        store.write(KEY_AUDIO_URI, uri)
        _config.value = _config.value.copy(audioUri = uri)
    }

    override suspend fun setRecordingEnabled(enabled: Boolean) {
        store.writeBool(KEY_RECORDING, enabled)
        _config.value = _config.value.copy(recordingEnabled = enabled)
    }

    companion object {
        private const val KEY_ENABLED = "auto_responder_enabled"
        private const val KEY_SCRIPT = "auto_responder_script"
        private const val KEY_AUDIO_URI = "auto_responder_audio_uri"
        private const val KEY_RECORDING = "auto_recording_enabled"
    }
}
