package org.carlospinan.bloqueador.app.telecom

import android.os.Handler
import android.os.Looper
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
        /**
         * The caller's contact name, or the label the user gave this number in their own rules.
         * Null until [setDisplayName] resolves one, and null forever for a number that is
         * neither — the screen falls back to the number itself.
         */
        val displayName: String? = null,
        /**
         * The DTMF tones sent on this call so far, in the order they were pressed.
         *
         * Kept here rather than in the composable so it survives the activity being recreated —
         * a rotation part-way through typing a card number would otherwise wipe the only record
         * of what had already been sent, and nothing on the line echoes it back.
         */
        val dtmfDigits: String = "",
    )

    private var call: Call? = null

    /**
     * Ends a DTMF tone a fixed time after it starts. A [Handler] rather than a coroutine scope
     * because this is an `object`: a scope built at class-init would need a main looper to exist
     * in every unit test that so much as touches this file, and this one is created lazily on the
     * first tone actually played.
     */
    private val dtmfHandler by lazy { Handler(Looper.getMainLooper()) }

    private val stopDtmfTone = Runnable { call?.stopDtmfTone() }

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
            // Before the reference goes: a tone still held when the call ends would otherwise
            // have its stop fired against whatever call is attached next.
            dtmfHandler.removeCallbacks(stopDtmfTone)
            call.stopDtmfTone()
            this.call = null
            _state.value = null
        }
    }

    fun setRepeatedCallAttempts(attempts: Int) {
        _state.value = _state.value?.copy(repeatedCallAttempts = attempts)
    }

    /**
     * Names the caller, once a lookup that could not be waited for has finished.
     *
     * [number] is checked against the call actually on screen rather than applied blindly: the
     * lookup runs off the call-setup path, so a call that ends and is replaced by another one
     * before it returns would otherwise put the previous caller's name on the new caller's
     * screen.
     */
    fun setDisplayName(
        number: String,
        name: String,
    ) {
        val current = _state.value ?: return
        if (current.number != number) return
        _state.value = current.copy(displayName = name)
    }

    fun answer() = call?.answer(0)

    fun decline() = call?.reject(false, null)

    fun hangUp() = call?.disconnect()

    /**
     * Sends one DTMF tone down the live call and records it on the screen.
     *
     * The tone is held for [DTMF_TONE_MILLIS] rather than stopped in the same frame. A tone below
     * roughly 40 ms is under what ITU Q.24 asks a receiver to recognise, and a menu that drops
     * every other digit is indistinguishable from a keypad that does not work. Any pending stop
     * is dropped first, so digits tapped in quick succession do not cut each other short —
     * `playDtmfTone` already ends whatever was playing before it.
     *
     * A no-op with no call attached, like every other action here: the pad is only reachable on
     * an active call, but the call can end between the press and the dispatch.
     */
    fun playDtmf(digit: Char) {
        val call = call ?: return
        val current = _state.value ?: return
        call.playDtmfTone(digit)
        dtmfHandler.removeCallbacks(stopDtmfTone)
        dtmfHandler.postDelayed(stopDtmfTone, DTMF_TONE_MILLIS)
        _state.value = current.copy(dtmfDigits = current.dtmfDigits + digit)
    }

    /** Comfortably above the ~40 ms an ITU Q.24 receiver has to accept, short enough to tap fast. */
    private const val DTMF_TONE_MILLIS = 250L

    private fun Int.toPhase(): CallUiPhase =
        when (this) {
            Call.STATE_RINGING -> CallUiPhase.RINGING
            Call.STATE_DIALING, Call.STATE_CONNECTING -> CallUiPhase.DIALING
            Call.STATE_ACTIVE -> CallUiPhase.ACTIVE
            else -> CallUiPhase.OTHER
        }
}
