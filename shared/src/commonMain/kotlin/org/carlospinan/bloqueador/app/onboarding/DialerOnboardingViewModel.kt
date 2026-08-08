package org.carlospinan.bloqueador.app.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.settings.SettingsRepository

data class DialerOnboardingUiState(
    val dialerState: DialerOnboardingState = DialerOnboardingState.NOT_REQUESTED,
    val welcomeShown: Boolean = false,
    val permissionsPromptShown: Boolean = false,
) {
    /** The role is what makes screening possible at all; without it the app is inert. */
    val dialerRoleHeld: Boolean
        get() =
            dialerState == DialerOnboardingState.GRANTED ||
                dialerState == DialerOnboardingState.ALREADY_DEFAULT
}

sealed interface DialerOnboardingIntent {
    data object WelcomeShown : DialerOnboardingIntent

    data object PermissionsPromptShown : DialerOnboardingIntent

    data object RequestStarted : DialerOnboardingIntent

    data class RequestResult(
        val granted: Boolean,
    ) : DialerOnboardingIntent

    data object Refresh : DialerOnboardingIntent
}

/**
 * Pure state machine driving the default-dialer permission onboarding screen.
 * The actual OS role/dialer-change intent is launched by the Android UI layer;
 * this class only tracks state transitions so they're unit-testable without
 * touching android.app.role.RoleManager (see docs/MILESTONES.md M1).
 *
 * Also owns [DialerOnboardingUiState.welcomeShown] -- MainActivity is the only
 * Android component that needs it (to decide whether to show the welcome screen
 * before onboarding), and this is the one ViewModel it legitimately owns at the root.
 */
class DialerOnboardingViewModel(
    private val gateway: DefaultDialerGateway,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            DialerOnboardingUiState(
                dialerState = if (gateway.isDefaultDialer()) DialerOnboardingState.ALREADY_DEFAULT else DialerOnboardingState.NOT_REQUESTED,
                welcomeShown = settingsRepository.welcomeShown,
                permissionsPromptShown = settingsRepository.permissionsPromptShown,
            ),
        )
    val state: StateFlow<DialerOnboardingUiState> = _state.asStateFlow()

    fun onIntent(intent: DialerOnboardingIntent) {
        when (intent) {
            DialerOnboardingIntent.WelcomeShown -> setWelcomeShown()
            DialerOnboardingIntent.PermissionsPromptShown -> setPermissionsPromptShown()
            DialerOnboardingIntent.RequestStarted -> onRequestStarted()
            is DialerOnboardingIntent.RequestResult -> onRequestResult(intent.granted)
            DialerOnboardingIntent.Refresh -> refresh()
        }
    }

    private fun setWelcomeShown() {
        viewModelScope.launch {
            settingsRepository.setWelcomeShown()
            _state.value = _state.value.copy(welcomeShown = true)
        }
    }

    private fun setPermissionsPromptShown() {
        viewModelScope.launch {
            settingsRepository.setPermissionsPromptShown()
            _state.value = _state.value.copy(permissionsPromptShown = true)
        }
    }

    private fun onRequestStarted() {
        when (_state.value.dialerState) {
            DialerOnboardingState.NOT_REQUESTED, DialerOnboardingState.DENIED ->
                _state.value = _state.value.copy(dialerState = DialerOnboardingState.REQUESTING)
            DialerOnboardingState.REQUESTING,
            DialerOnboardingState.GRANTED,
            DialerOnboardingState.ALREADY_DEFAULT,
            -> Unit
        }
    }

    private fun onRequestResult(granted: Boolean) {
        check(_state.value.dialerState == DialerOnboardingState.REQUESTING) {
            "onRequestResult received outside REQUESTING (was ${_state.value.dialerState})"
        }
        _state.value =
            _state.value.copy(
                dialerState = if (granted) DialerOnboardingState.GRANTED else DialerOnboardingState.DENIED,
            )
    }

    /**
     * Re-check on resume, in case the user changed the default dialer from system Settings
     * directly. This has to work in *both* directions: an earlier version only ever upgraded
     * to [DialerOnboardingState.ALREADY_DEFAULT], so a user who revoked the role kept seeing a
     * fully functional-looking app that could no longer screen a single call.
     *
     * [DialerOnboardingState.REQUESTING] is left alone -- the system role dialog is on screen
     * and its result, not this poll, decides what happens next.
     */
    private fun refresh() {
        if (_state.value.dialerState == DialerOnboardingState.REQUESTING) return

        val newState =
            when {
                gateway.isDefaultDialer() -> DialerOnboardingState.ALREADY_DEFAULT
                _state.value.dialerRoleHeld -> DialerOnboardingState.NOT_REQUESTED
                else -> _state.value.dialerState
            }
        _state.value = _state.value.copy(dialerState = newState)
    }
}
