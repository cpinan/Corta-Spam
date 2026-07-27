package org.carlospinan.bloqueador.app.autoresponder

import kotlinx.coroutines.flow.Flow

interface AutoResponderRepository {
    val config: Flow<AutoResponderConfig>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setScript(script: String)

    suspend fun setAudioUri(uri: String)

    suspend fun setRecordingEnabled(enabled: Boolean)
}
