package org.carlospinan.bloqueador.app.telecom

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
    /** Whether [start] should play the user's ringtone, vibrate, both, or neither. */
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
}
