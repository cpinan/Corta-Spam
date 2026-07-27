package org.carlospinan.bloqueador.app.autoresponder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutoResponderViewModel(
    private val repository: AutoResponderRepository,
) : ViewModel() {
    val config: StateFlow<AutoResponderConfig> =
        repository.config
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutoResponderConfig())

    val validationError: StateFlow<AutoResponderConfig.ErrorCode?> =
        repository.config
            .map { cfg ->
                when (val result = cfg.validate()) {
                    is AutoResponderConfig.ValidationResult.Ok -> null
                    is AutoResponderConfig.ValidationResult.Error -> result.code
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
