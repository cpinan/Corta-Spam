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
 *    Authoritative, needs no permission, and covers exactly the window this exists for.
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

    fun isExempt(
        exemptionEnabled: Boolean,
        inEmergencyCallbackMode: Boolean,
        nowMillis: Long,
        lastEmergencyCallAtMillis: Long,
    ): Boolean {
        if (!exemptionEnabled) return false
        if (inEmergencyCallbackMode) return true
        if (lastEmergencyCallAtMillis <= NEVER) return false
        // Not `elapsed in 0 until WINDOW`. A clock that has moved backwards since the emergency
        // call gives a negative elapsed, and the two ways to be wrong here are not the same size:
        // a spam call let through is a nuisance, an ambulance silently rejected is not.
        return nowMillis - lastEmergencyCallAtMillis < CALLBACK_WINDOW_MILLIS
    }
}
