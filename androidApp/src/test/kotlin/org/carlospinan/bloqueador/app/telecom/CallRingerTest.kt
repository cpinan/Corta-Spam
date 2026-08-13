package org.carlospinan.bloqueador.app.telecom

import android.content.Context
import android.media.AudioManager
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [RingerPolicyTest] proves the decision is right. This proves the decision is *acted on* — the
 * gap that has hidden six shipped-but-inert features in this project already. A policy object
 * whose result nobody consumes computes a perfect plan and leaves the phone silent.
 *
 * Vibration is what these assert, because it is the one half of ringing whose real effect
 * Robolectric records. The ringtone half needs `MediaPlayer` and remains device-only — see the
 * note on `ring_test.sh` at the bottom of this file.
 *
 * `application = Application::class` for the reason [ContactNameLookupTest] documents: the real
 * `CortaSpamApp.onCreate` calls `startKoin()`, and the second test in the JVM would then die.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class)
class CallRingerTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val audioManager: AudioManager
        get() = context.getSystemService(AudioManager::class.java)

    /** SDK 34 goes through `VibratorManager`, which is the branch [CallRinger.vibrator] takes. */
    private val vibratorShadow
        get() =
            Shadows.shadowOf(
                context.getSystemService(VibratorManager::class.java).defaultVibrator,
            )

    @Test
    fun `vibrate mode actually vibrates`() {
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        val ringer = CallRinger(context)

        ringer.start()

        assertTrue(vibratorShadow.isVibrating, "vibrate mode produced no vibration")
    }

    @Test
    fun `silent mode leaves the vibrator alone`() {
        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        val ringer = CallRinger(context)

        ringer.start()

        assertFalse(vibratorShadow.isVibrating)
    }

    @Test
    fun `normal mode vibrates alongside the ringtone`() {
        // On API 33+ the app can no longer read VIBRATE_WHEN_RINGING, so it does what the platform
        // dialer does and vibrates. This pins that, so the SDK branch cannot silently invert.
        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
        val ringer = CallRinger(context)

        ringer.start()

        assertTrue(vibratorShadow.isVibrating)
    }

    @Test
    fun `stop cancels a vibration that start began`() {
        // This is what silences a blocked call: the block decision lands mid-ring and calls stop().
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        val ringer = CallRinger(context)
        ringer.start()
        assertTrue(vibratorShadow.isVibrating, "precondition: the ringer must be vibrating")

        ringer.stop()

        assertFalse(vibratorShadow.isVibrating, "a blocked call would keep buzzing")
    }

    @Test
    fun `stop is safe when nothing ever started`() {
        // onCallRemoved calls stop() unconditionally, including for calls that never rang.
        CallRinger(context).stop()

        assertFalse(vibratorShadow.isVibrating)
    }

    @Test
    fun `a second start does not stack a second vibration`() {
        // Telecom can deliver onStateChanged(RINGING) more than once for one call.
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        val ringer = CallRinger(context)
        ringer.start()

        ringer.start()
        ringer.stop()

        // One cancel has to be enough. If start() had armed the vibrator twice, the guard in
        // startVibration() would be broken and a single stop() could leave it running.
        assertFalse(vibratorShadow.isVibrating)
    }
}

// Not covered here, deliberately: that a ringtone is audible. That needs a real MediaPlayer, a
// real ringtone URI and a real audio HAL, and the OEM behaviour it depends on is exactly what an
// emulator cannot stand in for. `./scripts/ring_test.sh watch --device <serial>` is the check
// that proves it, and it still has to be run on the razr.
