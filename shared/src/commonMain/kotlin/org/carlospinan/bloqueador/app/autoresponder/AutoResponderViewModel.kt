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

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(enabled) }
    }

    fun setScript(script: String) {
        viewModelScope.launch { repository.setScript(script) }
    }

    fun setAudioUri(uri: String) {
        viewModelScope.launch { repository.setAudioUri(uri) }
    }

    fun setRecordingEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setRecordingEnabled(enabled) }
    }

    fun clearAudioUri() {
        viewModelScope.launch { repository.setAudioUri("") }
    }
}
