package org.carlospinan.bloqueador.app.telecom

import android.app.NotificationManager
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

/**
 * The Do Not Disturb half of the same truth table.
 *
 * Declaring `IN_CALL_SERVICE_RINGING` took zen filtering away from Telecom and gave it to this
 * app, which never did it: `AudioManager.ringerMode` does not move when Do Not Disturb turns on,
 * so [RingerPolicy.plan] alone said "ring" for every call the user had asked not to hear. Both
 * directions are asserted here — a filtered caller must be silenced, and an allowed one must
 * still get through, because a fix that simply stopped ringing under DND would be the worse bug.
 */
class RingerPolicyDoNotDisturbTest {
    private fun priority(
        callsAllowed: Boolean = true,
        callSenders: Int = NotificationManager.Policy.PRIORITY_SENDERS_ANY,
        repeatCallersAllowed: Boolean = false,
    ) = RingerPolicy.DoNotDisturb(
        interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        policy =
            RingerPolicy.DoNotDisturb.Policy(
                callsAllowed = callsAllowed,
                callSenders = callSenders,
                repeatCallersAllowed = repeatCallersAllowed,
            ),
    )

    private val stranger = RingerPolicy.Caller(isContact = false, isStarred = false, isRepeatCaller = false)
    private val contact = RingerPolicy.Caller(isContact = true, isStarred = false, isRepeatCaller = false)
    private val favourite = RingerPolicy.Caller(isContact = true, isStarred = true, isRepeatCaller = false)
    private val redialler = RingerPolicy.Caller(isContact = false, isStarred = false, isRepeatCaller = true)

    @Test
    fun `do not disturb off rings without asking who is calling`() {
        assertEquals(RingerPolicy.Gate.RING, RingerPolicy.gate(RingerPolicy.DoNotDisturb.OFF))
    }

    @Test
    fun `total silence silences every caller`() {
        val dnd =
            RingerPolicy.DoNotDisturb(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_NONE,
                policy = null,
            )

        assertEquals(RingerPolicy.Gate.SILENCE, RingerPolicy.gate(dnd))
    }

    @Test
    fun `alarms only silences every caller`() {
        val dnd =
            RingerPolicy.DoNotDisturb(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALARMS,
                policy = null,
            )

        assertEquals(RingerPolicy.Gate.SILENCE, RingerPolicy.gate(dnd))
    }

    @Test
    fun `priority only with calls from anyone rings without an address book read`() {
        // The point of the RING verdict rather than ASK_CALLER: this is a common configuration
        // and it must not cost a contacts scan on the ringing path.
        assertEquals(RingerPolicy.Gate.RING, RingerPolicy.gate(priority()))
    }

    @Test
    fun `priority only with no call exception at all silences without asking`() {
        val dnd = priority(callsAllowed = false, repeatCallersAllowed = false)

        assertEquals(RingerPolicy.Gate.SILENCE, RingerPolicy.gate(dnd))
    }

    @Test
    fun `priority only restricted to contacts has to ask who is calling`() {
        val dnd = priority(callSenders = NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS)

        assertEquals(RingerPolicy.Gate.ASK_CALLER, RingerPolicy.gate(dnd))
        assertTrue(RingerPolicy.allowsCaller(dnd, contact))
        assertFalse(RingerPolicy.allowsCaller(dnd, stranger))
    }

    @Test
    fun `priority only restricted to favourites lets a starred contact through and no one else`() {
        val dnd = priority(callSenders = NotificationManager.Policy.PRIORITY_SENDERS_STARRED)

        assertTrue(RingerPolicy.allowsCaller(dnd, favourite))
        // A contact who is not starred is not a favourite, and this is the case that would be
        // wrong if `starred` were quietly treated as "is in the address book".
        assertFalse(RingerPolicy.allowsCaller(dnd, contact))
        assertFalse(RingerPolicy.allowsCaller(dnd, stranger))
    }

    @Test
    fun `a repeat caller gets through even when calls are otherwise blocked`() {
        // Android's own exception, and the reason gate() cannot return SILENCE on
        // callsAllowed=false alone.
        val dnd = priority(callsAllowed = false, repeatCallersAllowed = true)

        assertEquals(RingerPolicy.Gate.ASK_CALLER, RingerPolicy.gate(dnd))
        assertTrue(RingerPolicy.allowsCaller(dnd, redialler))
        assertFalse(RingerPolicy.allowsCaller(dnd, stranger))
    }

    @Test
    fun `a repeat caller is not let through when the user switched that exception off`() {
        val dnd = priority(callSenders = NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS, repeatCallersAllowed = false)

        assertFalse(RingerPolicy.allowsCaller(dnd, redialler))
    }

    @Test
    fun `an unreadable policy still silences a stranger and still rings a contact`() {
        // No notification-policy access: the filter is readable, its exceptions are not. This is
        // the common case on a real device, so it is the one that decides whether the reported
        // bug is actually fixed there.
        val dnd =
            RingerPolicy.DoNotDisturb(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                policy = null,
            )

        assertEquals(RingerPolicy.Gate.ASK_CALLER, RingerPolicy.gate(dnd))
        assertFalse(RingerPolicy.allowsCaller(dnd, stranger))
        assertTrue(RingerPolicy.allowsCaller(dnd, contact))
        assertTrue(RingerPolicy.allowsCaller(dnd, redialler))
    }

    @Test
    fun `a caller Do Not Disturb allows rings by the user's own ringer mode, not the zen-adjusted one`() {
        // The bug this exists for: on Android 16, AudioManager.getRingerMode() reports SILENT
        // while Do Not Disturb is on even though the user has the phone on normal. Feeding that
        // to plan() silenced the starred contacts and repeat callers Do Not Disturb had been
        // configured to let through -- a missed call the user had arranged to receive.
        val mode =
            RingerPolicy.effectiveRingerMode(
                audioManagerMode = AudioManager.RINGER_MODE_SILENT,
                userMode = AudioManager.RINGER_MODE_NORMAL,
                doNotDisturbActive = true,
            )

        assertEquals(AudioManager.RINGER_MODE_NORMAL, mode)
        assertTrue(RingerPolicy.plan(mode, vibrateWhileRinging = false).playRingtone)
    }

    @Test
    fun `a phone the user actually silenced stays silent under Do Not Disturb`() {
        // The reroute must not become a way to ring a genuinely silenced phone.
        val mode =
            RingerPolicy.effectiveRingerMode(
                audioManagerMode = AudioManager.RINGER_MODE_SILENT,
                userMode = AudioManager.RINGER_MODE_SILENT,
                doNotDisturbActive = true,
            )

        assertTrue(RingerPolicy.plan(mode, vibrateWhileRinging = true).isSilent)
    }

    @Test
    fun `with Do Not Disturb off AudioManager stays the source of truth`() {
        // It is the one that sees a volume-key press or a hardware switch immediately.
        val mode =
            RingerPolicy.effectiveRingerMode(
                audioManagerMode = AudioManager.RINGER_MODE_VIBRATE,
                userMode = AudioManager.RINGER_MODE_NORMAL,
                doNotDisturbActive = false,
            )

        assertEquals(AudioManager.RINGER_MODE_VIBRATE, mode)
    }

    @Test
    fun `an unknown interruption filter does not count as Do Not Disturb being on`() {
        val unknown =
            RingerPolicy.DoNotDisturb(
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_UNKNOWN,
                policy = null,
            )

        assertFalse(unknown.isActive)
        assertTrue(RingerPolicy.DoNotDisturb(NotificationManager.INTERRUPTION_FILTER_PRIORITY, null).isActive)
    }

    @Test
    fun `an unrecognised interruption filter rings`() {
        // Same stance as an unrecognised ringer mode: a value this code does not understand must
        // not be the reason a call arrives in silence.
        val dnd = RingerPolicy.DoNotDisturb(interruptionFilter = 99, policy = null)

        assertEquals(RingerPolicy.Gate.RING, RingerPolicy.gate(dnd))
    }
}
