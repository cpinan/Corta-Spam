package org.carlospinan.bloqueador.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.spam.SpamProviderRepository

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val spamProviderRepository: SpamProviderRepository,
) : ViewModel() {
    val blockingEnabled: StateFlow<Boolean> =
        settingsRepository.blockingEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoAllowContacts: StateFlow<Boolean> =
        settingsRepository.autoAllowContacts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val defaultAction: StateFlow<DefaultAction> =
        settingsRepository.defaultAction
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultAction.ALLOW)

    val spamEnabled: StateFlow<Boolean> =
        spamProviderRepository.enabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBlockingEnabled(enabled)
        }
    }

    fun setAutoAllowContacts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoAllowContacts(enabled)
        }
    }

    fun setDefaultAction(action: DefaultAction) {
        viewModelScope.launch {
            settingsRepository.setDefaultAction(action)
        }
    }

    fun setSpamEnabled(enabled: Boolean) {
        viewModelScope.launch {
            spamProviderRepository.setEnabled(enabled)
        }
    }
}
