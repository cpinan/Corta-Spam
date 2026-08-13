package org.carlospinan.bloqueador.app.telecom

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
 */
class CallRinger(
    private val context: Context,
) {
    private var player: MediaPlayer? = null
    private var vibrating = false

    private val audioManager: AudioManager?
        get() = context.getSystemService(AudioManager::class.java)

    private val vibrator: Vibrator?
        get() =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }

    fun start() {
        val plan =
            RingerPolicy.plan(
                ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL,
                vibrateWhileRinging = shouldVibrateWhileRinging(),
            )
        if (plan.playRingtone) startRingtone()
        if (plan.vibrate) startVibration()
    }

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
                            // USAGE_NOTIFICATION_RINGTONE is what routes this to the ring stream
                            // and lets it through Do Not Disturb's call exceptions.
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
