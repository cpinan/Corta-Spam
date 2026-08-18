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

    /** The platform's own signal outranks the timestamp and needs no emergency call on record. */
    @Test
    fun `emergency callback mode is exempt on its own`() {
        assertTrue(
            EmergencyCallPolicy.isExempt(
                exemptionEnabled = true,
                inEmergencyCallbackMode = true,
                nowMillis = now,
                lastEmergencyCallAtMillis = EmergencyCallPolicy.NEVER,
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
