package org.carlospinan.bloqueador.app.telecom

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.carlospinan.bloqueador.app.call.CallScreen

/**
 * Minimal in-call UI, launched by [PassthroughInCallService] because we
 * declare IN_CALL_SERVICE_UI in the manifest -- Telecom expects us, not the
 * system, to show incoming/active call UI once we're the default dialer.
 */
class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showOverLockScreen()

        setContent {
            val uiState by InCallState.state.collectAsState()
            val current = uiState
            if (current != null) {
                CallScreen(
                    number = current.number,
                    phase = current.phase,
                    onAnswer = InCallState::answer,
                    onDecline = InCallState::decline,
                    onHangUp = InCallState::hangUp,
                )
            } else {
                LaunchedEffect(Unit) { finish() }
            }
        }
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
}
