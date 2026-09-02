package org.carlospinan.bloqueador.app.telecom

import android.app.NotificationManager
import android.media.AudioManager

/**
 * What ringing an incoming call should actually do, given the phone's ringer mode.
 *
 * Split out of [CallRinger] for the same reason [NotificationPolicy] was split out of
 * [PassthroughInCallService]: the decision is four lines of policy wrapped in `MediaPlayer` and
 * `Vibrator` calls that no unit test can construct. Silence here is the worst bug this app can
 * ship — the user misses calls and has nothing to tell them why — and until now the only way to
 * exercise any of it was to place a real call.
 */
object RingerPolicy {
    /** Whether [CallRinger.start] should play the user's ringtone, vibrate, both, or neither. */
    data class RingerPlan(
        val playRingtone: Boolean,
        val vibrate: Boolean,
    ) {
        val isSilent: Boolean get() = !playRingtone && !vibrate
    }

    /**
     * Mirrors what the platform dialer does with each ringer mode. [vibrateWhileRinging] is only
     * consulted in [AudioManager.RINGER_MODE_NORMAL] — in vibrate mode the vibration *is* the
     * ring, so the setting cannot switch it off.
     *
     * An unrecognised mode rings normally. Failing loud is right for a phone: an unexpected value
     * from `AudioManager` must not be the reason a call arrives in silence.
     *
     * This says nothing about Do Not Disturb, and deliberately so — see [gate]. `ringerMode` does
     * not move when zen mode turns on, which is exactly why reading it alone let every call ring
     * straight through Do Not Disturb.
     */
    fun plan(
        ringerMode: Int,
        vibrateWhileRinging: Boolean,
    ): RingerPlan =
        when (ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> RingerPlan(playRingtone = false, vibrate = false)
            AudioManager.RINGER_MODE_VIBRATE -> RingerPlan(playRingtone = false, vibrate = true)
            else -> RingerPlan(playRingtone = true, vibrate = vibrateWhileRinging)
        }

    /**
     * Android's own definition of a repeat caller, and the window its Do Not Disturb exception
     * uses: the same number calling again within fifteen minutes.
     */
    const val REPEAT_CALLER_WINDOW_MILLIS = 15L * 60L * 1000L

    /**
     * The phone's Do Not Disturb configuration, as `NotificationManager` reports it.
     *
     * This app declares `android.telecom.IN_CALL_SERVICE_RINGING`, so Telecom does not ring on
     * its behalf and does not apply zen filtering on its behalf either — the app took the whole
     * job, ringtone and vibration alike, and has to do the Do Not Disturb half itself. Nothing
     * did, which is why a spam call buzzed a phone whose owner had asked for silence.
     */
    data class DoNotDisturb(
        /** One of `NotificationManager.INTERRUPTION_FILTER_*`. */
        val interruptionFilter: Int,
        /**
         * The priority-callers rules, or null when they could not be read.
         *
         * `NotificationManager.getNotificationPolicy()` throws unless the user has granted this
         * app notification-policy access, which is a separate trip into system Settings that a
         * dialer has no other reason to ask for. Null therefore means "Do Not Disturb is on and
         * we cannot see its exceptions", not "there are none" — see [assumedPolicy].
         */
        val policy: Policy?,
    ) {
        /**
         * Whether zen mode is filtering anything at all.
         *
         * `INTERRUPTION_FILTER_UNKNOWN` counts as off: it is what a device reports when it cannot
         * answer, and treating "don't know" as "filtering" would reroute the ringer mode below on
         * a phone with no Do Not Disturb active.
         */
        val isActive: Boolean
            get() =
                interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                    interruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN

        /** The subset of `NotificationManager.Policy` that decides whether a call may ring. */
        data class Policy(
            val callsAllowed: Boolean,
            /** One of `NotificationManager.Policy.PRIORITY_SENDERS_*`. */
            val callSenders: Int,
            val repeatCallersAllowed: Boolean,
        )

        companion object {
            /** Do Not Disturb is off: every call rings, subject only to the ringer mode. */
            val OFF =
                DoNotDisturb(
                    interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                    policy = null,
                )

            /**
             * What to assume when the filter is "priority only" but the exceptions are unreadable.
             *
             * Modelled on the platform default, which is the configuration nearly every user is
             * actually in: calls from contacts get through, and so does anyone who calls twice.
             * Guessing "allow everything" would leave the reported bug in place; guessing "allow
             * nothing" would silence the user's own family on a permission they were never asked
             * for. Letting known callers through is the only guess whose failure mode is a call
             * the user wanted rather than one they explicitly asked not to hear.
             */
            val assumedPolicy =
                Policy(
                    callsAllowed = true,
                    callSenders = NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS,
                    repeatCallersAllowed = true,
                )
        }
    }

    /**
     * What a priority-only filter needs to know about the caller — and the only thing it needs.
     *
     * Answering any of these costs an address-book scan or a database read, which is why [gate]
     * exists to decide whether they have to be asked for at all.
     */
    data class Caller(
        val isContact: Boolean,
        val isStarred: Boolean,
        val isRepeatCaller: Boolean,
    )

    /** Whether the ringer may start now, must stay silent, or needs [allowsCaller] answered first. */
    enum class Gate {
        RING,
        SILENCE,
        ASK_CALLER,
    }

    /**
     * The cheap half of the decision: everything that can be answered without touching the
     * address book or the database, on the path where the phone is already ringing.
     *
     * An unrecognised filter rings, for the reason [plan] rings on an unrecognised ringer mode.
     */
    fun gate(dnd: DoNotDisturb): Gate =
        when (dnd.interruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            -> Gate.SILENCE

            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> {
                val policy = dnd.policy ?: DoNotDisturb.assumedPolicy
                when {
                    !policy.callsAllowed && !policy.repeatCallersAllowed -> Gate.SILENCE
                    policy.callsAllowed &&
                        policy.callSenders == NotificationManager.Policy.PRIORITY_SENDERS_ANY -> Gate.RING
                    else -> Gate.ASK_CALLER
                }
            }

            else -> Gate.RING
        }

    /**
     * Which ringer mode to ring by, once [gate] and [allowsCaller] have decided this call may ring.
     *
     * **`AudioManager.getRingerMode()` lies while Do Not Disturb is on.** It reports
     * `RINGER_MODE_SILENT` for the zen-adjusted state, not for what the user chose — on an
     * Android 16 emulator with the phone on normal and Do Not Disturb set to priority-only,
     * `getRingerMode()` returns 0 while `Settings.Global.MODE_RINGER` still reads 2. Feeding the
     * first of those to [plan] silences every call, including the starred contacts and repeat
     * callers Do Not Disturb was explicitly configured to let through. That is the same bug as
     * ringing through Do Not Disturb, pointed the other way, and it is the worse one: a missed
     * call the user had arranged to receive.
     *
     * So while zen is filtering, the user's own setting is the honest answer to "is this phone on
     * silent" — the zen adjustment has already been accounted for by the gate, and applying it
     * twice is what silences the exceptions. The stream itself is not muted: only `STREAM_SYSTEM`
     * is, so a player on the ring stream is still audible.
     *
     * With Do Not Disturb off the two agree, and `AudioManager` stays the source of truth because
     * it is the one that tracks a hardware switch or a volume-key press immediately.
     */
    fun effectiveRingerMode(
        audioManagerMode: Int,
        userMode: Int,
        doNotDisturbActive: Boolean,
    ): Int = if (doNotDisturbActive) userMode else audioManagerMode

    /**
     * The expensive half: whether *this* caller is one of the exceptions the user allowed through.
     *
     * The repeat-caller exception is checked first because it is independent of the sender rules —
     * Android lets a second call within [REPEAT_CALLER_WINDOW_MILLIS] through even when calls are
     * otherwise blocked entirely, on the reasoning that someone redialling is trying to reach you.
     */
    fun allowsCaller(
        dnd: DoNotDisturb,
        caller: Caller,
    ): Boolean {
        val policy = dnd.policy ?: DoNotDisturb.assumedPolicy
        if (policy.repeatCallersAllowed && caller.isRepeatCaller) return true
        if (!policy.callsAllowed) return false
        return when (policy.callSenders) {
            NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS -> caller.isContact
            NotificationManager.Policy.PRIORITY_SENDERS_STARRED -> caller.isStarred
            // PRIORITY_SENDERS_ANY, and anything the platform adds later: ring rather than
            // silence, matching every other unrecognised-value branch in this file.
            else -> true
        }
    }
}
