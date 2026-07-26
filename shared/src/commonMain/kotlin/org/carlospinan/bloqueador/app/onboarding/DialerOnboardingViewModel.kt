package org.carlospinan.bloqueador.app.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pure state machine driving the default-dialer permission onboarding screen.
 * The actual OS role/dialer-change intent is launched by the Android UI layer;
 * this class only tracks state transitions so they're unit-testable without
 * touching android.app.role.RoleManager (see docs/MILESTONES.md M1).
 */
class DialerOnboardingViewModel(
    private val gateway: DefaultDialerGateway,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            if (gateway.isDefaultDialer()) DialerOnboardingState.ALREADY_DEFAULT else DialerOnboardingState.NOT_REQUESTED,
        )
    val state: StateFlow<DialerOnboardingState> = _state.asStateFlow()

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
