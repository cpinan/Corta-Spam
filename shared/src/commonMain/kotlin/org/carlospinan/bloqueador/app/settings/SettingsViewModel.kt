package org.carlospinan.bloqueador.app.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.spam.SpamProviderRepository

data class SettingsUiState(
    val blockingEnabled: Boolean = true,
    val autoAllowContacts: Boolean = true,
    val defaultAction: DefaultAction = DefaultAction.ALLOW,
    val spamEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val spamProviderRepository: SpamProviderRepository,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.blockingEnabled,
            settingsRepository.autoAllowContacts,
            settingsRepository.defaultAction,
            spamProviderRepository.enabled,
            settingsRepository.notificationsEnabled,
        ) { blockingEnabled, autoAllowContacts, defaultAction, spamEnabled, notificationsEnabled ->
            SettingsUiState(
                blockingEnabled = blockingEnabled,
                autoAllowContacts = autoAllowContacts,
                defaultAction = defaultAction,
                spamEnabled = spamEnabled,
                notificationsEnabled = notificationsEnabled,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

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

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }
}
