package org.carlospinan.bloqueador.app.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.settings.SettingsRepository

/**
 * Pure state machine driving the default-dialer permission onboarding screen.
 * The actual OS role/dialer-change intent is launched by the Android UI layer;
 * this class only tracks state transitions so they're unit-testable without
 * touching android.app.role.RoleManager (see docs/MILESTONES.md M1).
 *
 * Also owns [welcomeShown] -- MainActivity is the only Android component that
 * needs it (to decide whether to show the welcome screen before onboarding),
 * and this is the one ViewModel it legitimately owns at the root.
 */
class DialerOnboardingViewModel(
    private val gateway: DefaultDialerGateway,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            if (gateway.isDefaultDialer()) DialerOnboardingState.ALREADY_DEFAULT else DialerOnboardingState.NOT_REQUESTED,
        )
    val state: StateFlow<DialerOnboardingState> = _state.asStateFlow()

    private val _welcomeShown = MutableStateFlow(settingsRepository.welcomeShown)
    val welcomeShown: StateFlow<Boolean> = _welcomeShown.asStateFlow()

    fun setWelcomeShown() {
        viewModelScope.launch {
            settingsRepository.setWelcomeShown()
            _welcomeShown.value = true
        }
    }

    fun onRequestStarted() {
        when (_state.value) {
            DialerOnboardingState.NOT_REQUESTED, DialerOnboardingState.DENIED ->
                _state.value = DialerOnboardingState.REQUESTING
            DialerOnboardingState.REQUESTING,
            DialerOnboardingState.GRANTED,
            DialerOnboardingState.ALREADY_DEFAULT,
            -> Unit
        }
    }

    fun onRequestResult(granted: Boolean) {
        check(_state.value == DialerOnboardingState.REQUESTING) {
            "onRequestResult received outside REQUESTING (was ${_state.value})"
        }
        _state.value = if (granted) DialerOnboardingState.GRANTED else DialerOnboardingState.DENIED
    }

    /** Re-check on resume, in case the user changed the default dialer from system Settings directly. */
    fun refresh() {
        if (gateway.isDefaultDialer()) {
            _state.value = DialerOnboardingState.ALREADY_DEFAULT
        }
    }
}
