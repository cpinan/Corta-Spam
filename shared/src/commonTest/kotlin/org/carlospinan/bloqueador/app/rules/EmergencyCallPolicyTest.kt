package org.carlospinan.bloqueador.app.rules

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The app's own defaults were dangerous before this existed. A callback from an ambulance service
 * arrives from a number that is not in the address book, so with the default action set to BLOCK,
 * or inside quiet hours, or under a country rule, it was blocked — and with the auto-responder on
 * it was answered, read a greeting, and hung up on, with nothing to tell the user it had happened.
 */
class EmergencyCallPolicyTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `a call inside the window after an emergency call is exempt`() {
        assertTrue(
            EmergencyCallPolicy.isExempt(
                exemptionEnabled = true,
                inEmergencyCallbackMode = false,
                nowMillis = now,
                lastEmergencyCallAtMillis = now - 60_000L,
            ),
        )
    }

    @Test
    fun `the window closes`() {
        assertFalse(
            EmergencyCallPolicy.isExempt(
                exemptionEnabled = true,
                inEmergencyCallbackMode = false,
                nowMillis = now,
                lastEmergencyCallAtMillis = now - EmergencyCallPolicy.CALLBACK_WINDOW_MILLIS - 1,
            ),
        )
    }

    /** The platform's own signal needs no emergency call on record — only a start time. */
    @Test
    fun `emergency callback mode is exempt on its own`() {
        assertTrue(
            EmergencyCallPolicy.isExempt(
                exemptionEnabled = true,
                inEmergencyCallbackMode = true,
                nowMillis = now,
                lastEmergencyCallAtMillis = EmergencyCallPolicy.NEVER,
                callbackModeSinceMillis = now,
            ),
        )
    }

    /**
     * A platform that never clears callback mode must not disable blocking forever.
     *
     * This is the case that made the bound necessary rather than tidy. An emulator that dialled
     * 112 once reported `PROPERTY_EMERGENCY_CALLBACK_MODE` on every incoming call for at least a
     * day afterwards, and every rule — manual blocks included — was short-circuited for as long as
     * it did. Android's own callback mode is about five minutes, so a flag still set after thirty
     * is stuck, not a long emergency.
     */
    @Test
    fun `callback mode stops exempting once its own window has passed`() {
        assertFalse(
            EmergencyCallPolicy.isExempt(
                exemptionEnabled = true,
                inEmergencyCallbackMode = true,
                nowMillis = now,
                lastEmergencyCallAtMillis = EmergencyCallPolicy.NEVER,
                callbackModeSinceMillis = now - EmergencyCallPolicy.CALLBACK_WINDOW_MILLIS - 1,
            ),
        )
    }

    /** Inside its window it still exempts, which is the whole point of keeping the signal. */
    @Test
    fun `callback mode still exempts inside its window`() {
        assertTrue(
            EmergencyCallPolicy.isExempt(
                exemptionEnabled = true,
                inEmergencyCallbackMode = true,
                nowMillis = now,
                lastEmergencyCallAtMillis = EmergencyCallPolicy.NEVER,
                callbackModeSinceMillis = now - EmergencyCallPolicy.CALLBACK_WINDOW_MILLIS + 1,
            ),
        )
    }

    /**
     * A stuck flag must not suppress the other signal.
     *
     * With callback mode long expired but a real emergency call dialled a minute ago, the call is
     * still exempt — on the timestamp this app recorded itself.
     */
    @Test
    fun `an expired callback mode does not hide a recent emergency call`() {
        assertTrue(
            EmergencyCallPolicy.isExempt(
                exemptionEnabled = true,
                inEmergencyCallbackMode = true,
                nowMillis = now,
                lastEmergencyCallAtMillis = now - 60_000L,
                callbackModeSinceMillis = now - EmergencyCallPolicy.CALLBACK_WINDOW_MILLIS - 1,
            ),
        )
    }

    @Test
    fun `switching the exemption off turns off both signals`() {
        assertFalse(
            EmergencyCallPolicy.isExempt(
                exemptionEnabled = false,
                inEmergencyCallbackMode = true,
                nowMillis = now,
                lastEmergencyCallAtMillis = now,
            ),
        )
    }

    @Test
    fun `no emergency call on record is not exempt`() {
        assertFalse(
            EmergencyCallPolicy.isExempt(
                exemptionEnabled = true,
                inEmergencyCallbackMode = false,
                nowMillis = now,
                lastEmergencyCallAtMillis = EmergencyCallPolicy.NEVER,
            ),
        )
    }

    /**
     * A clock that moves backwards between the emergency call and the callback — a time-zone or
     * NTP correction — must not close the window. The two ways to be wrong are not the same size.
     */
    @Test
    fun `a clock that went backwards keeps the exemption`() {
        assertTrue(
            EmergencyCallPolicy.isExempt(
                exemptionEnabled = true,
                inEmergencyCallbackMode = false,
                nowMillis = now - 5 * 60 * 60 * 1000L,
                lastEmergencyCallAtMillis = now,
            ),
        )
    }
}
