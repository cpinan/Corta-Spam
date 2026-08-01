package org.carlospinan.bloqueador.app.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Handles the Answer/Decline actions from [IncomingCallNotifier]'s full-screen call notification. */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_ANSWER -> InCallState.answer()
            ACTION_DECLINE -> InCallState.decline()
            ACTION_HANG_UP -> InCallState.hangUp()
        }
        IncomingCallNotifier.cancel(context)
        if (intent.action == ACTION_HANG_UP) {
            IncomingCallNotifier.cancelOngoing(context)
        }
    }

    companion object {
        const val ACTION_ANSWER = "org.carlospinan.bloqueador.app.telecom.ACTION_ANSWER"
        const val ACTION_DECLINE = "org.carlospinan.bloqueador.app.telecom.ACTION_DECLINE"
        const val ACTION_HANG_UP = "org.carlospinan.bloqueador.app.telecom.ACTION_HANG_UP"
    }
}
