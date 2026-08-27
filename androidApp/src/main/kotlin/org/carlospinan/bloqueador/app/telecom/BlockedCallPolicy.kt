package org.carlospinan.bloqueador.app.telecom

import org.carlospinan.bloqueador.app.call.CallUiPhase

/**
 * How a call the rules blocked is ended, and how long the app waits before forcing it.
 *
 * Split out of [PassthroughInCallService] for the reason [RingerPolicy], [NotificationPolicy],
 * [CallDirectionPolicy] and [ProximityPolicy] were: the service cannot be constructed in a unit
 * test, and this decision is the difference between a blocked call ending and a blocked call the
 * user is unknowingly connected to.
 *
 * The bug it exists to fix: the service called `Call.reject()` on every blocked call and never
 * looked again. `reject()` only does anything to a *ringing* call — a call already answered (the
 * ringing screen has an Answer button and is deliberately not blanked by [ProximityPolicy], so a
 * pocket can reach it; a headset button or the system UI can do it too) swallows the reject
 * silently. The "Blocked call" notification was posted either way, so the user was told the call
 * had been blocked while it was live on their phone, on the loudspeaker.
 */
internal object BlockedCallPolicy {
    enum class Termination {
        /** Ringing: refuse it, so the caller gets a rejection rather than a dropped connection. */
        REJECT,

        /** Anything already connected: rejecting is a no-op, so hang up instead. */
        DISCONNECT,

        /** Already on its way out; touching it again would only race Telecom. */
        ALREADY_ENDING,
    }

    fun terminationFor(phase: CallUiPhase): Termination =
        when (phase) {
            CallUiPhase.RINGING -> Termination.REJECT
            CallUiPhase.DISCONNECTING -> Termination.ALREADY_ENDING
            // ACTIVE, HOLDING, DIALING and OTHER all get hung up. OTHER especially: an unknown
            // state is not a reason to leave a blocked call connected.
            else -> Termination.DISCONNECT
        }

    /**
     * How long after asking for a blocked call to end before the app checks that it did.
     *
     * Long enough that a normal rejection has been through Telecom and back as `onCallRemoved`,
     * short enough that a caller the user believes is blocked is not listening to their room.
     */
    const val TERMINATION_TIMEOUT_MILLIS = 3_000L

    /**
     * How long a call answered for the auto-responder may stay silent before it is hung up.
     *
     * Text-to-speech is the part that fails: an engine that reports an initialisation failure, a
     * missing voice for the device language, or a `speak()` that returns an error never calls back
     * at all. Before this, that left the call answered indefinitely with the loudspeaker on —
     * which is what a user reports as "it answered itself and I could hear the call in progress".
     *
     * Generous because a cold TTS engine on a mid-range device can take seconds to bind.
     */
    const val GREETING_START_TIMEOUT_MILLIS = 10_000L

    /**
     * The ceiling on a greeting that did start but never reported finishing — a playback thread
     * that dies, or an engine that drops the utterance callback.
     *
     * Above the ~35 s a full-length script ([org.carlospinan.bloqueador.app.autoresponder.AutoResponderConfig.MAX_SCRIPT_LENGTH]
     * characters) takes to read aloud, so it never truncates a working greeting.
     */
    const val GREETING_MAX_MILLIS = 60_000L
}
