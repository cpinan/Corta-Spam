package org.carlospinan.bloqueador.app.testing

import kotlinx.coroutines.flow.MutableStateFlow
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderConfig
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderRepository

/**
 * In-memory [AutoResponderRepository] for tests.
 *
 * Setters write through to [config], so a test can either seed a starting config or assert
 * that a ViewModel intent reached the repository.
 */
internal class FakeAutoResponderRepository(
    override val config: MutableStateFlow<AutoResponderConfig> = MutableStateFlow(AutoResponderConfig()),
) : AutoResponderRepository {
    override suspend fun setEnabled(enabled: Boolean) {
        config.value = config.value.copy(enabled = enabled)
    }

    override suspend fun setScript(script: String) {
        config.value = config.value.copy(script = script)
    }

    override suspend fun setAudioUri(uri: String) {
        config.value = config.value.copy(audioUri = uri)
    }

    override suspend fun setRecordingEnabled(enabled: Boolean) {
        config.value = config.value.copy(recordingEnabled = enabled)
    }
}
