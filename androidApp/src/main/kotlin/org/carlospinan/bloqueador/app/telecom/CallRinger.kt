package org.carlospinan.bloqueador.app.telecom

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Plays the incoming-call ringtone and vibration.
 *
 * The manifest declares `android.telecom.IN_CALL_SERVICE_RINGING`, which tells Telecom that this
 * app rings for itself — and Telecom then does not ring. Nothing in the app ever played a
 * ringtone, so as the default dialer it took every call in silence. That is the single worst
 * failure mode a phone app has: the user misses calls and has no way to tell why.
 *
 * Keeping the declaration (rather than handing ringing back to the system) is what lets a
 * blocked call stay silent: [stop] is called the moment a block decision lands, usually before
 * the first ring finishes.
 *
 * The system ringer mode is honoured exactly as the platform dialer would: silent rings not at
 * all, vibrate rings only through the vibrator, normal plays the user's chosen ringtone and also
 * vibrates when they've asked for vibrate-while-ringing.
 *
 * **Do Not Disturb is not this class's decision, but it is this class's fault when it is wrong.**
 * Taking over ringing took over zen filtering with it, and neither the ringtone (played through
 * `MediaPlayer`) nor the vibration (a direct `Vibrator` call) is filtered by the platform on an
 * app's behalf. [start] therefore rings unconditionally, and the caller is expected to have asked
 * [doNotDisturb] and [RingerPolicy.gate] first — see [PassthroughInCallService].
 */
class CallRinger(
    private val context: Context,
) {
    private var player: MediaPlayer? = null
    private var vibrating = false

    private val audioManager: AudioManager?
        get() = context.getSystemService(AudioManager::class.java)

    private val notificationManager: NotificationManager?
        get() = context.getSystemService(NotificationManager::class.java)

    /**
     * The phone's current Do Not Disturb configuration, for [RingerPolicy.gate] to rule on.
     *
     * Reading it is this class's job rather than the caller's for the same reason reading
     * `ringerMode` is: everything that touches a system service lives here, and everything that
     * decides anything lives in [RingerPolicy], where a unit test can reach it.
     *
     * `getNotificationPolicy()` is the part that can fail. It throws `SecurityException` unless
     * the user has granted notification-policy access — a separate trip into system Settings that
     * this app never asks for — so the exceptions are frequently unreadable even though the
     * filter itself always is. That is a null policy, not an absent one; see
     * [RingerPolicy.DoNotDisturb.assumedPolicy] for what gets assumed in its place.
     */
    fun doNotDisturb(): RingerPolicy.DoNotDisturb {
        val manager = notificationManager ?: return RingerPolicy.DoNotDisturb.OFF
        val filter =
            runCatching { manager.currentInterruptionFilter }
                .getOrElse {
                    Log.w(TAG, "Could not read the Do Not Disturb filter; ringing normally", it)
                    return RingerPolicy.DoNotDisturb.OFF
                }
        val policy =
            runCatching { manager.notificationPolicy }
                .getOrNull()
                ?.let {
                    RingerPolicy.DoNotDisturb.Policy(
                        callsAllowed =
                            it.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_CALLS != 0,
                        callSenders = it.priorityCallSenders,
                        repeatCallersAllowed =
                            it.priorityCategories and
                                NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS != 0,
                    )
                }
        return RingerPolicy.DoNotDisturb(interruptionFilter = filter, policy = policy)
    }

    private val vibrator: Vibrator?
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }

    /**
     * [dnd] is the same reading the caller gated on, passed in rather than re-read so the ringtone
     * is chosen by the state the decision was made against — and because while zen is filtering,
     * `AudioManager` is not the honest source for the ringer mode. See
     * [RingerPolicy.effectiveRingerMode].
     */
    fun start(dnd: RingerPolicy.DoNotDisturb = RingerPolicy.DoNotDisturb.OFF) {
        val plan =
            RingerPolicy.plan(
                ringerMode =
                    RingerPolicy.effectiveRingerMode(
                        audioManagerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL,
                        userMode = userRingerMode(),
                        doNotDisturbActive = dnd.isActive,
                    ),
                vibrateWhileRinging = shouldVibrateWhileRinging(),
            )
        if (plan.playRingtone) startRingtone()
        if (plan.vibrate) startVibration()
    }

    /**
     * The ringer mode the user chose, as opposed to the one zen mode is currently imposing.
     *
     * `Settings.Global.MODE_RINGER` is world-readable and needs no permission. Falling back to
     * `AudioManager` on a device that does not publish it keeps the previous behaviour rather
     * than inventing silence.
     */
    private fun userRingerMode(): Int =
        runCatching {
            android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.MODE_RINGER,
            )
        }.getOrElse { audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL }

    fun stop() {
        player?.let { active ->
            runCatching {
                if (active.isPlaying) active.stop()
            }
            active.release()
        }
        player = null
        if (vibrating) {
            vibrator?.cancel()
            vibrating = false
        }
    }

    private fun startRingtone() {
        if (player != null) return
        val uri =
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: return
        try {
            player =
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            // USAGE_NOTIFICATION_RINGTONE is what routes this to the ring stream,
                            // so the ring volume the user set is the volume it plays at. It does
                            // NOT make zen mode police it -- an app playing its own ringtone
                            // through MediaPlayer is not filtered by Do Not Disturb at all, which
                            // is why start() is only reached once RingerPolicy has allowed it.
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    setDataSource(context, uri)
                    isLooping = true
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "Ringtone playback failed ($what, $extra)")
                        stop()
                        true
                    }
                    setOnPreparedListener { start() }
                    prepareAsync()
                }
        } catch (e: Exception) {
            // A missing or unreadable ringtone must not take the call down with it: the
            // full-screen UI and the vibration are still there to announce the call.
            Log.e(TAG, "Could not start the ringtone", e)
            player?.release()
            player = null
        }
    }

    private fun startVibration() {
        if (vibrating) return
        val device = vibrator?.takeIf { it.hasVibrator() } ?: return
        // 1s buzz, 1s pause, repeating from index 0 until cancel() -- the familiar ring cadence.
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0)
        val attributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        @Suppress("DEPRECATION")
        device.vibrate(effect, attributes)
        vibrating = true
    }

    private fun shouldVibrateWhileRinging(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Settings.System.VIBRATE_WHEN_RINGING is no longer readable by apps; the platform
            // dialer's behaviour on modern releases is to vibrate alongside the ringtone.
            true
        } else {
            @Suppress("DEPRECATION")
            android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.VIBRATE_WHEN_RINGING,
                0,
            ) == 1
        }

    private companion object {
        const val TAG = "CallRinger"
    }
}
