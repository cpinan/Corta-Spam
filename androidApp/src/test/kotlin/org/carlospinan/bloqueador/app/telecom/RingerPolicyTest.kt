package org.carlospinan.bloqueador.app.telecom

import android.media.AudioManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The ringing truth table. Asserted positively — "vibrate mode vibrates" rather than "silent mode
 * doesn't crash" — because the failure this guards against is silence, and an absence assertion
 * passes just as happily against a ringer that does nothing at all.
 */
class RingerPolicyTest {
    @Test
    fun `silent mode neither rings nor vibrates`() {
        val plan = RingerPolicy.plan(AudioManager.RINGER_MODE_SILENT, vibrateWhileRinging = true)

        assertFalse(plan.playRingtone)
        assertFalse(plan.vibrate)
        assertTrue(plan.isSilent)
    }

    @Test
    fun `vibrate mode vibrates without playing the ringtone`() {
        val plan = RingerPolicy.plan(AudioManager.RINGER_MODE_VIBRATE, vibrateWhileRinging = false)

        assertFalse(plan.playRingtone)
        // Not gated on vibrateWhileRinging: in vibrate mode the vibration *is* the ring, so the
        // setting that suppresses vibrating-alongside-a-ringtone must not reach it.
        assertTrue(plan.vibrate)
    }

    @Test
    fun `normal mode plays the ringtone`() {
        val plan = RingerPolicy.plan(AudioManager.RINGER_MODE_NORMAL, vibrateWhileRinging = false)

        assertTrue(plan.playRingtone)
        assertFalse(plan.vibrate)
    }

    @Test
    fun `normal mode also vibrates when the user asked for it`() {
        val plan = RingerPolicy.plan(AudioManager.RINGER_MODE_NORMAL, vibrateWhileRinging = true)

        assertTrue(plan.playRingtone)
        assertTrue(plan.vibrate)
    }

    @Test
    fun `an unrecognised ringer mode still rings`() {
        // Failing loud on purpose. The app declares IN_CALL_SERVICE_RINGING, so Telecom does not
        // ring on its behalf: a mode this code does not understand must not become a silent call.
        val plan = RingerPolicy.plan(ringerMode = 99, vibrateWhileRinging = false)

        assertTrue(plan.playRingtone)
        assertFalse(plan.isSilent)
    }

    @Test
    fun `only silent mode is silent`() {
        val modes =
            listOf(
                AudioManager.RINGER_MODE_SILENT,
                AudioManager.RINGER_MODE_VIBRATE,
                AudioManager.RINGER_MODE_NORMAL,
            )

        val silent = modes.filter { RingerPolicy.plan(it, vibrateWhileRinging = false).isSilent }

        assertEquals(listOf(AudioManager.RINGER_MODE_SILENT), silent)
    }
}
