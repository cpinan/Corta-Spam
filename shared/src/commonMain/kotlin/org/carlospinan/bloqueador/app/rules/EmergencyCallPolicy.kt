package org.carlospinan.bloqueador.app.rules

/**
 * Whether an incoming call is protected from every blocking rule because the user has just called
 * the emergency services.
 *
 * Without this, the app's own defaults were dangerous. A callback from an ambulance service
 * arrives from a number that is not in the address book, so with the default action set to BLOCK,
 * or inside a quiet-hours window, or under a country rule, it was blocked — and with the
 * auto-responder switched on it was *answered, read a greeting, and hung up on*. The user would
 * have no idea it happened.
 *
 * Two signals, in order of trust:
 *
 * 1. [inEmergencyCallbackMode] — the platform's own `Call.Details.PROPERTY_EMERGENCY_CALLBACK_MODE`.
 *    Needs no permission, and covers exactly the window this exists for — for as long as the
 *    platform is honest about it, which is why it is time-boxed like the other one rather than
 *    trusted outright.
 * 2. [lastEmergencyCallAtMillis] — when this app last saw the user dial a number
 *    [EmergencyNumbers] recognises. The fallback for networks that never enter callback mode.
 *
 * Neither can identify a callback that arrives after the window has closed. Doing that would mean
 * `TelephonyManager.isEmergencyNumber` and `READ_PHONE_STATE`, which this app deliberately does
 * not hold. The exemption is therefore time-boxed and says so.
 */
object EmergencyCallPolicy {
    /**
     * How long after an emergency call every incoming call is let through.
     *
     * Thirty minutes: long enough for a dispatcher to ring back after a queue, short enough that a
     * user who called 112 in the morning is not unprotected all day. Android's own emergency
     * callback mode is typically five minutes, so this is the more generous of the two and the
     * platform signal wins whenever it is present anyway.
     */
    const val CALLBACK_WINDOW_MILLIS: Long = 30L * 60L * 1000L

    /** Nothing recorded. Distinct from "recorded at epoch zero", which no real clock produces. */
    const val NEVER: Long = 0L

    /**
     * [callbackModeSinceMillis] is when this app *first* saw [inEmergencyCallbackMode] set on the
     * current stretch of it, or [NEVER] if it is not currently set. The caller maintains it; see
     * `EvaluateIncomingCallUseCase`.
     *
     * It exists because the platform signal used to be trusted without a time bound, and a
     * platform that never clears it therefore switched call blocking off permanently — the whole
     * purpose of the app, disabled silently, with the only trace a `rule_detail` in the call log.
     * That is not hypothetical: an emulator that dialled 112 once reported callback mode on every
     * incoming call for at least a day afterwards, while `dumpsys telephony.registry` reported
     * `mECBMReason=0`. Android's own callback mode is typically five minutes, so a signal still
     * set after thirty is a stuck flag rather than a long emergency.
     *
     * Both paths are now bounded by the same [CALLBACK_WINDOW_MILLIS], measured from the first
     * moment this app had reason to believe an emergency was in progress. The window restarts if
     * callback mode clears and returns, which is what a second emergency looks like.
     */
    fun isExempt(
        exemptionEnabled: Boolean,
        inEmergencyCallbackMode: Boolean,
        nowMillis: Long,
        lastEmergencyCallAtMillis: Long,
        callbackModeSinceMillis: Long = NEVER,
    ): Boolean {
        if (!exemptionEnabled) return false
        // Not `elapsed in 0 until WINDOW`. A clock that has moved backwards since the emergency
        // call gives a negative elapsed, and the two ways to be wrong here are not the same size:
        // a spam call let through is a nuisance, an ambulance silently rejected is not.
        if (inEmergencyCallbackMode && withinWindow(nowMillis, callbackModeSinceMillis)) return true
        return withinWindow(nowMillis, lastEmergencyCallAtMillis)
    }

    private fun withinWindow(
        nowMillis: Long,
        sinceMillis: Long,
    ): Boolean {
        if (sinceMillis <= NEVER) return false
        return nowMillis - sinceMillis < CALLBACK_WINDOW_MILLIS
    }
}
