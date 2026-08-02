package org.carlospinan.bloqueador.app.autoresponder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AutoResponderUiState(
    val config: AutoResponderConfig = AutoResponderConfig(),
    val validationError: AutoResponderConfig.ErrorCode? = null,
)

sealed interface AutoResponderIntent {
    data class SetEnabled(
        val enabled: Boolean,
    ) : AutoResponderIntent

    data class SetScript(
        val script: String,
    ) : AutoResponderIntent

    data class SetAudioUri(
        val uri: String,
    ) : AutoResponderIntent

    data class SetRecordingEnabled(
        val enabled: Boolean,
    ) : AutoResponderIntent

    data object ClearAudioUri : AutoResponderIntent
}

class AutoResponderViewModel(
    private val repository: AutoResponderRepository,
) : ViewModel() {
    val state: StateFlow<AutoResponderUiState> =
        repository.config
            .map { config ->
                val validationError =
                    when (val result = config.validate()) {
                        is AutoResponderConfig.ValidationResult.Ok -> null
                        is AutoResponderConfig.ValidationResult.Error -> result.code
                    }
                AutoResponderUiState(config = config, validationError = validationError)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutoResponderUiState())

    fun onIntent(intent: AutoResponderIntent) {
        when (intent) {
            is AutoResponderIntent.SetEnabled -> setEnabled(intent.enabled)
            is AutoResponderIntent.SetScript -> setScript(intent.script)
            is AutoResponderIntent.SetAudioUri -> setAudioUri(intent.uri)
            is AutoResponderIntent.SetRecordingEnabled -> setRecordingEnabled(intent.enabled)
            AutoResponderIntent.ClearAudioUri -> clearAudioUri()
        }
    }

    private fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(enabled) }
    }

    private fun setScript(script: String) {
        viewModelScope.launch { repository.setScript(script) }
    }

    private fun setAudioUri(uri: String) {
        viewModelScope.launch { repository.setAudioUri(uri) }
    }

    private fun setRecordingEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setRecordingEnabled(enabled) }
    }

    private fun clearAudioUri() {
        viewModelScope.launch { repository.setAudioUri("") }
    }
}
