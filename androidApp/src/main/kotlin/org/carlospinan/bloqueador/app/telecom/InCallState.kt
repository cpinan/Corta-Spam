package org.carlospinan.bloqueador.app.telecom

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.carlospinan.bloqueador.app.call.CallUiPhase

/**
 * Bridges the single active [Call] Telecom hands to [PassthroughInCallService]
 * into UI state for [InCallActivity]. M1 scope: one call at a time, no
 * hold/merge/conference/waiting-call handling.
 */
object InCallState {
    data class UiState(
        val number: String,
        val phase: CallUiPhase,
        val repeatedCallAttempts: Int? = null,
    )

    private var call: Call? = null

    private val _state = MutableStateFlow<UiState?>(null)
    val state: StateFlow<UiState?> = _state.asStateFlow()

    private val callback =
        object : Call.Callback() {
            override fun onStateChanged(
                call: Call,
                newState: Int,
            ) {
                _state.value = _state.value?.copy(phase = newState.toPhase())
            }
        }

    fun attach(call: Call) {
        this.call = call
        call.registerCallback(callback)
        _state.value =
            UiState(
                number =
                    call.details
                        ?.handle
                        ?.schemeSpecificPart
                        .orEmpty(),
                phase = call.state.toPhase(),
            )
    }

    fun detach(call: Call) {
        call.unregisterCallback(callback)
        if (this.call === call) {
            this.call = null
            _state.value = null
        }
    }

    fun setRepeatedCallAttempts(attempts: Int) {
        _state.value = _state.value?.copy(repeatedCallAttempts = attempts)
    }

    fun answer() = call?.answer(0)

    fun decline() = call?.reject(false, null)

    fun hangUp() = call?.disconnect()

    private fun Int.toPhase(): CallUiPhase =
        when (this) {
            Call.STATE_RINGING -> CallUiPhase.RINGING
            Call.STATE_DIALING, Call.STATE_CONNECTING -> CallUiPhase.DIALING
            Call.STATE_ACTIVE -> CallUiPhase.ACTIVE
            else -> CallUiPhase.OTHER
        }
}
