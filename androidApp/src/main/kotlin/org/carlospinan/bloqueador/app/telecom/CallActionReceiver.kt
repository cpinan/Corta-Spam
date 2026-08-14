package org.carlospinan.bloqueador.app.telecom

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
            ACTION_CALL_BACK -> callBack(context, intent)
        }
        if (intent.action == ACTION_ANSWER || intent.action == ACTION_DECLINE || intent.action == ACTION_HANG_UP) {
            IncomingCallNotifier.cancel(context)
        }
        if (intent.action == ACTION_HANG_UP) {
            IncomingCallNotifier.cancelOngoing(context)
        }
    }

    /**
     * Returns a call from its notification.
     *
     * Places it directly only when `CALL_PHONE` is already granted. Without the permission the
     * fallback is the app's own keypad, pre-filled with the number: a `BroadcastReceiver` cannot
     * show a permission dialog, and firing `ACTION_CALL` without the grant throws a
     * `SecurityException` that would look to the user like the button doing nothing at all.
     */
    private fun callBack(
        context: Context,
        intent: Intent,
    ) {
        val number = intent.getStringExtra(EXTRA_NUMBER)
        if (number.isNullOrBlank()) return
        IncomingCallNotifier.cancelCallResult(context, number)

        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
                PackageManager.PERMISSION_GRANTED
        val action = if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL
        // '+' is left unescaped deliberately: it is meaningful in a tel: number and encoding it
        // to %2B breaks every international call. '#' still has to be escaped, or everything
        // after it reads as a fragment.
        val callIntent =
            Intent(action, "tel:${Uri.encode(number.trim(), "+")}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!granted) {
            // ACTION_DIAL would otherwise be offered to every dialer on the device, including the
            // one this app replaced -- which is exactly the trip to another app being fixed here.
            callIntent.setPackage(context.packageName)
        }
        try {
            context.startActivity(callIntent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No activity could handle the call-back intent", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "CALL_PHONE was revoked between the permission check and the call", e)
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
        const val ACTION_CALL_BACK = "org.carlospinan.bloqueador.app.telecom.ACTION_CALL_BACK"
        const val EXTRA_NUMBER = "org.carlospinan.bloqueador.app.telecom.EXTRA_NUMBER"
    }
}
