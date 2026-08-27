package org.carlospinan.bloqueador.app.telecom

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.carlospinan.bloqueador.app.call.CallUiPhase

/**
 * Bridges the [Call]s Telecom hands to [PassthroughInCallService] into UI state for
 * [InCallActivity].
 *
 * **One of them is on screen; all of them are tracked.** This used to hold a single `Call` in a
 * single field, and call waiting broke it in a way that left the user stranded: a second call
 * overwrote the first, and when that second call ended `detach` saw it was the one it held and set
 * the state to null — so the activity finished while the *first* call was still connected. The
 * ongoing-call notification survived, but its Hang up action routed back here to a null field and
 * did nothing. The only way to end the call was the system's own UI.
 *
 * So [CallStack] holds the set Telecom has given us and which one is being shown, and losing the
 * shown call promotes rather than clears. A ringing call wins the promotion, because a call the user
 * has to answer or decline is more urgent than one they are already on.
 *
 * Still out of scope, and deliberately: merging, conferencing, and swapping between two calls the
 * user is holding. Those are features. This is the difference between the phone app working and
 * the phone app disappearing.
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
        /**
         * How many calls Telecom is holding besides the one on screen. Almost always 0; 1 when a
         * second call arrives during the first. The screen says so rather than silently showing
         * one of two calls as though it were the only one.
         */
        val otherCallCount: Int = 0,
        /**
         * Seconds since Telecom reported the call connected, or null while it has not. Ticked
         * once a second by [syncTicker] rather than derived in the composable, so the value the
         * screen renders is the same one a test can set.
         */
        val callDurationSeconds: Int? = null,
        /** Whether the microphone is muted, as reported by Telecom — never as last requested. */
        val muted: Boolean = false,
        /** Whether the audio route is the loudspeaker. Same reasoning as [muted]. */
        val speakerOn: Boolean = false,
    )

    /** Per-call, so promoting a call back to the screen restores what was known about it. */
    private class CallInfo {
        var displayName: String? = null
        var repeatedCallAttempts: Int? = null
        var dtmfDigits: String = ""
    }

    /** Which calls exist and which is on screen. See [CallStack] for why it is a separate type. */
    private val stack = CallStack<Call> { it.state == Call.STATE_RINGING }
    private val info = mutableMapOf<Call, CallInfo>()

    private val primary: Call? get() = stack.primary

    /**
     * Ends a DTMF tone a fixed time after it starts. A [Handler] rather than a coroutine scope
     * because this is an `object`: a scope built at class-init would need a main looper to exist
     * in every unit test that so much as touches this file, and this one is created lazily on the
     * first tone actually played.
     */
    private val dtmfHandler by lazy { Handler(Looper.getMainLooper()) }

    /** The call a held tone belongs to — not necessarily the one on screen by the time it fires. */
    private var dtmfCall: Call? = null

    private val stopDtmfTone =
        Runnable {
            dtmfCall?.stopDtmfTone()
            dtmfCall = null
        }

    private val _state = MutableStateFlow<UiState?>(null)
    val state: StateFlow<UiState?> = _state.asStateFlow()

    /**
     * The live `InCallService`, for the two controls that are not on the [Call] — muting and the
     * audio route are properties of the whole call session, so Telecom puts them on the service.
     *
     * Held as a nullable field and cleared by [clear] because this is an `object` and the service
     * is not: a reference kept past `onDestroy` is both a leak and a handle on a dead binding.
     */
    private var service: InCallService? = null

    /**
     * Ticks the call timer. Separate from [dtmfHandler] so that stopping one cannot cancel the
     * other, and lazy for the same reason: a unit test that never reaches an active call must not
     * need a main looper.
     */
    private val tickHandler by lazy { Handler(Looper.getMainLooper()) }

    private val tick =
        object : Runnable {
            override fun run() {
                val current = _state.value ?: return
                val call = primary ?: return
                _state.value = current.copy(callDurationSeconds = call.durationSeconds())
                tickHandler.postDelayed(this, TICK_INTERVAL_MILLIS)
            }
        }

    private val callback =
        object : Call.Callback() {
            override fun onStateChanged(
                call: Call,
                newState: Int,
            ) {
                // A disconnected call is over, and waiting for `onCallRemoved` to say so is what
                // stranded the user on a dead call screen. Telecom holds a call in
                // STATE_DISCONNECTED for as long as it likes before removing it -- seconds on a
                // real network -- and this screen renders that state with no buttons at all, while
                // Back deliberately backgrounds the task instead of leaving. The call had ended
                // and there was nothing to press and no way out. Ending the call ends the screen.
                if (newState == Call.STATE_DISCONNECTED) {
                    detach(call)
                    return
                }
                // Guarded on identity: this callback is registered on every call, and without the
                // check a background call changing state would rewrite the phase of the call the
                // user is looking at.
                if (call !== primary) return
                _state.value = _state.value?.copy(phase = newState.toCallUiPhase())
                syncTicker()
            }
        }

    /**
     * Runs the call timer exactly while there is a connected call to time.
     *
     * Driven off the phase rather than started once at connect: a call that goes on hold and comes
     * back is the same call, and a timer left running through the hold would have counted time the
     * user was not on it.
     */
    private fun syncTicker() {
        if (_state.value?.phase == CallUiPhase.ACTIVE) {
            tickHandler.removeCallbacks(tick)
            ticking = true
            tick.run()
        } else {
            stopTicking()
        }
    }

    /** Guarded on [ticking] so a call that never connected does not build the handler at all. */
    private fun stopTicking() {
        if (!ticking) return
        tickHandler.removeCallbacks(tick)
        ticking = false
    }

    private var ticking = false

    /** Remembers the service the [Call]s came from, so mute and the audio route are reachable. */
    fun attachService(service: InCallService) {
        this.service = service
    }

    /**
     * Records what Telecom says the audio session is doing.
     *
     * The screen renders this rather than what the last tap asked for, because the two are not the
     * same: the auto-responder moves the route to the loudspeaker by itself, a headset being
     * plugged in moves it back, and a button drawn from a local guess would show the opposite of
     * the truth in both cases.
     */
    fun onAudioStateChanged(audioState: CallAudioState) {
        _state.value =
            _state.value?.copy(
                muted = audioState.isMuted,
                speakerOn = audioState.route == CallAudioState.ROUTE_SPEAKER,
            )
    }

    /** Flips the microphone. A no-op with no service bound, like every other action here. */
    fun toggleMute() {
        val current = _state.value ?: return
        service?.setMuted(!current.muted)
    }

    /**
     * Moves the audio between the loudspeaker and the earpiece.
     *
     * `ROUTE_WIRED_OR_EARPIECE` rather than `ROUTE_EARPIECE` on the way back: with a headset
     * plugged in, asking for the earpiece specifically sends the call to the handset the user is
     * not holding to their ear.
     */
    fun toggleSpeaker() {
        val current = _state.value ?: return
        service?.setAudioRoute(
            if (current.speakerOn) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_SPEAKER,
        )
    }

    fun attach(call: Call) {
        stack.add(call)
        info.getOrPut(call) { CallInfo() }
        call.registerCallback(callback)
        show(call)
    }

    fun detach(call: Call) {
        call.unregisterCallback(callback)
        info.remove(call)
        if (dtmfCall === call) {
            // A tone still held when its call ends would otherwise be stopped against whatever
            // call is on screen by the time the handler runs.
            dtmfHandler.removeCallbacks(stopDtmfTone)
            call.stopDtmfTone()
            dtmfCall = null
        }

        when (stack.remove(call)) {
            // A background call ending changes nothing on screen except that it is no longer
            // there to mention.
            CallStack.Removal.Unchanged ->
                _state.value = _state.value?.copy(otherCallCount = stack.otherCount())

            CallStack.Removal.Empty -> {
                _state.value = null
                stopTicking()
            }

            CallStack.Removal.Promoted -> stack.primary?.let { show(it) }
        }
    }

    /**
     * Drops every call and takes the screen down with them.
     *
     * For [PassthroughInCallService.onDestroy]. The `Call` objects belong to the service's binding
     * to Telecom, so once that is gone `disconnect()` on them reaches nothing — and this is an
     * `object`, which outlives the service. Without this, a service torn down while a call was
     * still attached left a call screen the user could not leave and a hang-up button that did
     * nothing, and the next call would promote that dead one back onto the screen when it ended.
     */
    fun clear() {
        info.keys.forEach { it.unregisterCallback(callback) }
        info.clear()
        // Guarded rather than unconditional: touching the handler builds it, and it is lazy
        // precisely so a test that never plays a tone needs no main looper.
        if (dtmfCall != null) {
            dtmfHandler.removeCallbacks(stopDtmfTone)
            dtmfCall = null
        }
        stopTicking()
        stack.clear()
        service = null
        _state.value = null
    }

    /** Puts [call] on screen, restoring whatever was already known about it. */
    private fun show(call: Call) {
        val callInfo = info.getOrPut(call) { CallInfo() }
        // Carried across rather than reset: mute and the route belong to the audio session, not to
        // whichever call is on screen, so promoting the other call must not draw an un-muted
        // microphone over a muted one.
        val audio = _state.value
        _state.value =
            UiState(
                number = call.handleNumber(),
                phase = call.state.toCallUiPhase(),
                repeatedCallAttempts = callInfo.repeatedCallAttempts,
                displayName = callInfo.displayName,
                dtmfDigits = callInfo.dtmfDigits,
                otherCallCount = stack.otherCount(),
                callDurationSeconds = call.durationSeconds(),
                muted = audio?.muted ?: false,
                speakerOn = audio?.speakerOn ?: false,
            )
        syncTicker()
    }

    /**
     * How long this call has been connected, or null if it has not been.
     *
     * `connectTimeMillis` is wall clock, which is what Telecom exposes publicly — the monotonic
     * `connectElapsedTimeMillis` is `@hide`. A clock change mid-call would therefore skew the
     * timer; that is a worse trade for a two-minute call than having no timer at all.
     */
    private fun Call.durationSeconds(): Int? {
        val connectedAt = details?.connectTimeMillis ?: 0L
        if (connectedAt <= 0L) return null
        return ((System.currentTimeMillis() - connectedAt) / 1000L).coerceAtLeast(0L).toInt()
    }

    /**
     * Records that [number] has been calling repeatedly.
     *
     * Keyed by number rather than applied to whatever is on screen: evaluation runs off the
     * call-setup path, so with a second call in progress the result for one caller could otherwise
     * land on the other caller's screen.
     */
    fun setRepeatedCallAttempts(
        number: String,
        attempts: Int,
    ) {
        val call = callFor(number) ?: return
        info.getOrPut(call) { CallInfo() }.repeatedCallAttempts = attempts
        if (call === primary) {
            _state.value = _state.value?.copy(repeatedCallAttempts = attempts)
        }
    }

    /**
     * Names the caller, once a lookup that could not be waited for has finished.
     *
     * Stored against the call it belongs to, not against the screen. The lookup runs off the
     * call-setup path: a name resolved for a call that has since been backgrounded by a second one
     * used to be discarded outright, so the name never appeared even after that second call ended
     * and the first came back to the screen.
     */
    fun setDisplayName(
        number: String,
        name: String,
    ) {
        val call = callFor(number) ?: return
        info.getOrPut(call) { CallInfo() }.displayName = name
        if (call === primary) {
            _state.value = _state.value?.copy(displayName = name)
        }
    }

    private fun callFor(number: String): Call? = info.keys.firstOrNull { it.handleNumber() == number }

    fun answer() = primary?.answer(0)

    fun decline() = primary?.reject(false, null)

    fun hangUp() = primary?.disconnect()

    /** Takes a held call off hold. A no-op unless something actually put it there. */
    fun resume() = primary?.unhold()

    /**
     * Sends one DTMF tone down the call on screen and records it there.
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
        val call = primary ?: return
        val current = _state.value ?: return
        call.playDtmfTone(digit)
        dtmfHandler.removeCallbacks(stopDtmfTone)
        dtmfCall = call
        dtmfHandler.postDelayed(stopDtmfTone, DTMF_TONE_MILLIS)
        val digits = current.dtmfDigits + digit
        info.getOrPut(call) { CallInfo() }.dtmfDigits = digits
        _state.value = current.copy(dtmfDigits = digits)
    }

    /** Comfortably above the ~40 ms an ITU Q.24 receiver has to accept, short enough to tap fast. */
    private const val DTMF_TONE_MILLIS = 250L

    /** One second, because the timer shows seconds. */
    private const val TICK_INTERVAL_MILLIS = 1_000L

    private fun Call.handleNumber(): String =
        details
            ?.handle
            ?.schemeSpecificPart
            .orEmpty()
}
