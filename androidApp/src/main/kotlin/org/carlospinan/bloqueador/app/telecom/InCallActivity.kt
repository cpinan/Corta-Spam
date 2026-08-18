package org.carlospinan.bloqueador.app.telecom

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.carlospinan.bloqueador.app.call.CallScreen
import org.carlospinan.bloqueador.app.call.CallUiPhase

/**
 * Minimal in-call UI, launched by [PassthroughInCallService] because we
 * declare IN_CALL_SERVICE_UI in the manifest -- Telecom expects us, not the
 * system, to show incoming/active call UI once we're the default dialer.
 */
class InCallActivity : ComponentActivity() {
    /**
     * Turns the screen off while the phone is against a face.
     *
     * Null on a device with no proximity sensor, which is what `isWakeLockLevelSupported` is
     * asked about: `newWakeLock` succeeds regardless and hands back a lock that never fires, so
     * without the check there is nothing to distinguish "no sensor" from "sensor says nothing is
     * near", and the release path below would be guessing.
     *
     * Built lazily rather than in `onCreate` so a call that never connects never touches
     * `PowerManager` at all.
     */
    private val proximityLock: PowerManager.WakeLock? by lazy {
        val powerManager = getSystemService(PowerManager::class.java) ?: return@lazy null
        if (!powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            Log.d(TAG, "No proximity sensor on this device; the screen stays on during calls")
            return@lazy null
        }
        powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, PROXIMITY_LOCK_TAG)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        enableEdgeToEdge()
        showOverLockScreen()
        dismissKeyguard()

        setContent {
            val uiState by InCallState.state.collectAsState()
            val current = uiState

            if (current != null) {
                // Back must not end the call UI *while there is a call*. It used to `finish()`
                // this activity while the call carried on, and as the default dialer there is no
                // second app holding a call screen -- the user was left on a live call with
                // nothing showing it. This backgrounds the task instead, exactly as pressing Home
                // does, so the screen is still there to come back to.
                //
                // Once the call is ending there is nothing left to come back to, and backgrounding
                // is the wrong answer: Telecom can hold a disconnected call for seconds before it
                // removes it, and for that whole time Back was refusing to leave a screen about a
                // call that had already ended. Then it leaves.
                val ending = current.phase == CallUiPhase.DISCONNECTING && current.otherCallCount == 0
                BackHandler { if (ending) finish() else moveTaskToBack(true) }

                // Keyed on the answer, so the lock follows the call: switching to the loudspeaker
                // mid-call gives the screen back immediately, and so does going on hold.
                //
                // The answer is also remembered, because the call is only half of the condition —
                // see onResume. Found on a device: leaving the screen with Home released the lock
                // (correctly), and coming back to the still-connected call never re-acquired it,
                // because this effect's key had not changed while it was away.
                val blank = ProximityPolicy.shouldBlankScreen(current.phase, current.speakerOn)
                LaunchedEffect(blank) {
                    proximityWanted = blank
                    if (blank) acquireProximityLock() else releaseProximityLock()
                }

                CallScreen(
                    number = current.number,
                    phase = current.phase,
                    onAnswer = InCallState::answer,
                    onDecline = InCallState::decline,
                    onHangUp = InCallState::hangUp,
                    onResume = InCallState::resume,
                    repeatedCallAttempts = current.repeatedCallAttempts,
                    displayName = current.displayName,
                    dtmfDigits = current.dtmfDigits,
                    onDtmf = InCallState::playDtmf,
                    otherCallCount = current.otherCallCount,
                    callDurationSeconds = current.callDurationSeconds,
                    muted = current.muted,
                    onToggleMute = InCallState::toggleMute,
                    speakerOn = current.speakerOn,
                    onToggleSpeaker = InCallState::toggleSpeaker,
                    onDismiss = ::finish,
                )
            } else {
                LaunchedEffect(Unit) {
                    Log.d(TAG, "State is null, finishing")
                    finish()
                }
            }
        }
    }

    /**
     * Whether the *call* wants the screen blanked, as last decided by [ProximityPolicy].
     *
     * Held separately from the lock because the condition has two halves that change
     * independently: what the call is doing, and whether this screen is the thing in front of the
     * user. The lock is taken only when both are true.
     */
    private var proximityWanted = false

    /** The call may have wanted the screen blanked the whole time this screen was away. */
    override fun onResume() {
        super.onResume()
        if (proximityWanted) acquireProximityLock()
    }

    /**
     * The screen belongs to whatever is in front of the user, and once this activity is not, the
     * lock has to go — otherwise leaving a call screen with Home, while the call carries on, would
     * keep blanking the launcher every time the phone came near anything.
     */
    override fun onPause() {
        super.onPause()
        releaseProximityLock()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        releaseProximityLock()
    }

    private fun acquireProximityLock() {
        val lock = proximityLock ?: return
        // Reference-counting is off by default on a wake lock built this way, but acquire() on a
        // held lock is still a no-op worth not making: the effect below re-runs on every state
        // change that keeps the answer the same.
        if (lock.isHeld) return
        lock.acquire()
        Log.d(TAG, "Proximity lock acquired")
    }

    private fun releaseProximityLock() {
        val lock = proximityLock ?: return
        if (!lock.isHeld) return
        // RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY, so hanging up with the phone still at the ear does
        // not flash the screen on against the user's face. The screen comes back when the handset
        // does, which is when they can see it.
        lock.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
        Log.d(TAG, "Proximity lock released")
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    // No pre-O branch: minSdk is 26, so requestDismissKeyguard is always available and the
    // FLAG_DISMISS_KEYGUARD fallback was unreachable.
    private fun dismissKeyguard() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        keyguardManager?.requestDismissKeyguard(this, null)
    }

    companion object {
        private const val TAG = "InCallActivity"

        /** Shows up in `dumpsys power` as the holder, so a lock left behind names its owner. */
        private const val PROXIMITY_LOCK_TAG = "CortaSpam:InCallProximity"
    }
}
