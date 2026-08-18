package org.carlospinan.bloqueador.app.telecom

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
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
    }
}
