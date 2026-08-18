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
    val notifyUnknownCallers: Boolean = true,
    val repeatedCallerBypassCount: Int = 0,
    val emergencyCallbackExemption: Boolean = true,
)

sealed interface SettingsIntent {
    data class SetBlockingEnabled(
        val enabled: Boolean,
    ) : SettingsIntent

    data class SetAutoAllowContacts(
        val enabled: Boolean,
    ) : SettingsIntent

    data class SetDefaultAction(
        val action: DefaultAction,
    ) : SettingsIntent

    data class SetSpamEnabled(
        val enabled: Boolean,
    ) : SettingsIntent

    data class SetNotificationsEnabled(
        val enabled: Boolean,
    ) : SettingsIntent

    data class SetNotifyUnknownCallers(
        val enabled: Boolean,
    ) : SettingsIntent

    data class SetRepeatedCallerBypassCount(
        val count: Int,
    ) : SettingsIntent

    data class SetEmergencyCallbackExemption(
        val enabled: Boolean,
    ) : SettingsIntent
}

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
        }.combine(settingsRepository.notifyUnknownCallers) { partial, notifyUnknownCallers ->
            partial.copy(notifyUnknownCallers = notifyUnknownCallers)
        }.combine(settingsRepository.emergencyCallbackExemption) { partial, emergencyCallbackExemption ->
            partial.copy(emergencyCallbackExemption = emergencyCallbackExemption)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetBlockingEnabled -> setBlockingEnabled(intent.enabled)
            is SettingsIntent.SetAutoAllowContacts -> setAutoAllowContacts(intent.enabled)
            is SettingsIntent.SetDefaultAction -> setDefaultAction(intent.action)
            is SettingsIntent.SetSpamEnabled -> setSpamEnabled(intent.enabled)
            is SettingsIntent.SetNotificationsEnabled -> setNotificationsEnabled(intent.enabled)
            is SettingsIntent.SetNotifyUnknownCallers -> setNotifyUnknownCallers(intent.enabled)
            is SettingsIntent.SetRepeatedCallerBypassCount -> setRepeatedCallerBypassCount(intent.count)
            is SettingsIntent.SetEmergencyCallbackExemption -> setEmergencyCallbackExemption(intent.enabled)
        }
    }

    private fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBlockingEnabled(enabled)
        }
    }

    private fun setAutoAllowContacts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoAllowContacts(enabled)
        }
    }

    private fun setDefaultAction(action: DefaultAction) {
        viewModelScope.launch {
            settingsRepository.setDefaultAction(action)
        }
    }

    private fun setSpamEnabled(enabled: Boolean) {
        viewModelScope.launch {
            spamProviderRepository.setEnabled(enabled)
        }
    }

    private fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    private fun setNotifyUnknownCallers(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifyUnknownCallers(enabled)
        }
    }

    private fun setRepeatedCallerBypassCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.setRepeatedCallerBypassCount(count)
        }
    }

    private fun setEmergencyCallbackExemption(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEmergencyCallbackExemption(enabled)
        }
    }
}
