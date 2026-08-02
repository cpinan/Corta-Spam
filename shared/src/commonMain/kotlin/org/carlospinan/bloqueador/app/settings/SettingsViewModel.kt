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
    val repeatedCallerBypassCount: Int = 0,
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
            // kotlinx.coroutines' typed combine() overloads top out at 5 flows; chaining a plain
            // 2-arg combine on top avoids forcing the mixed-type set into the vararg Array<T> form.
        }.combine(settingsRepository.repeatedCallerBypassCount) { partial, repeatedCallerBypassCount ->
            partial.copy(repeatedCallerBypassCount = repeatedCallerBypassCount)
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

    fun setRepeatedCallerBypassCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.setRepeatedCallerBypassCount(count)
        }
    }
}
