package org.carlospinan.bloqueador.app.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.carlospinan.bloqueador.app.rules.RuleRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Handles the Answer/Decline actions from [IncomingCallNotifier]'s full-screen call notification,
 * and the Block/Allow rule buttons on a finished call's notification.
 */
class CallActionReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val ruleRepository: RuleRepository by inject()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_ANSWER -> InCallState.answer()
            ACTION_DECLINE -> InCallState.decline()
            ACTION_HANG_UP -> InCallState.hangUp()
            ACTION_BLOCK_NUMBER -> applyRule(context, intent, block = true)
            ACTION_ALLOW_NUMBER -> applyRule(context, intent, block = false)
        }
        if (intent.action == ACTION_ANSWER || intent.action == ACTION_DECLINE || intent.action == ACTION_HANG_UP) {
            IncomingCallNotifier.cancel(context)
        }
        if (intent.action == ACTION_HANG_UP) {
            IncomingCallNotifier.cancelOngoing(context)
        }
    }

    /**
     * Writes the rule off the main thread, holding the broadcast open until it lands.
     *
     * `goAsync()` is what makes that safe: without it the process is eligible to die the moment
     * `onReceive` returns, and the insert would be lost about as often as the phone is busy. The
     * notification is dismissed only after the write, so a tap that did not take does not look
     * like one that did.
     */
    private fun applyRule(
        context: Context,
        intent: Intent,
        block: Boolean,
    ) {
        val number = intent.getStringExtra(EXTRA_NUMBER)
        if (number.isNullOrBlank()) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (block) {
                    ruleRepository.addBlockedNumber(number)
                } else {
                    ruleRepository.addAllowlistedNumber(number)
                }
                IncomingCallNotifier.cancelCallResult(context, number)
            } catch (e: Exception) {
                // The notification deliberately stays up: it is the only surface telling the user
                // this number needs a decision, and silently dismissing it would report a rule
                // that was never written.
                Log.e(TAG, "Could not save the rule for a number from its notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "CallActionReceiver"
        const val ACTION_ANSWER = "org.carlospinan.bloqueador.app.telecom.ACTION_ANSWER"
        const val ACTION_DECLINE = "org.carlospinan.bloqueador.app.telecom.ACTION_DECLINE"
        const val ACTION_HANG_UP = "org.carlospinan.bloqueador.app.telecom.ACTION_HANG_UP"
        const val ACTION_BLOCK_NUMBER = "org.carlospinan.bloqueador.app.telecom.ACTION_BLOCK_NUMBER"
        const val ACTION_ALLOW_NUMBER = "org.carlospinan.bloqueador.app.telecom.ACTION_ALLOW_NUMBER"
        const val EXTRA_NUMBER = "org.carlospinan.bloqueador.app.telecom.EXTRA_NUMBER"
    }
}
