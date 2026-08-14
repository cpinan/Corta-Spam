package org.carlospinan.bloqueador.app.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.TelecomManager
import android.util.Log
import org.carlospinan.bloqueador.app.R

/**
 * Stops Telecom posting its own missed-call notification next to ours.
 *
 * The user was getting two notifications for one missed call: the app's, and the platform's.
 * Telecom's rule is in `MissedCallNotifierImpl.shouldManageNotificationThroughDefaultDialer` --
 * **if the default dialer declares a receiver for this action, Telecom stays out of it and
 * delegates; if it does not, Telecom posts its own.** The test is the mere existence of the
 * receiver in the manifest, not anything it does.
 *
 * That matters here, because this receiver will usually never run. Telecom sends the broadcast
 * with `sendBroadcastAsUser(..., READ_PHONE_STATE)`, and this app deliberately does not hold
 * `READ_PHONE_STATE` -- see the manifest, where not declaring it is a Play-policy decision this
 * project has already paid for once. So the *suppression* is what is being bought here, and the
 * notification the user actually sees is the one
 * [PassthroughInCallService.onCallRemoved] posts from the missed call itself.
 *
 * The body is still implemented rather than left empty, for the case where the broadcast does
 * arrive (a system that sends it without the permission, or a future grant): a missed call that
 * this app somehow did not see live is exactly the case Telecom is reporting, and posting
 * nothing would be strictly worse than the duplicate this receiver exists to remove.
 */
class MissedCallNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in HANDLED_ACTIONS) return
        val number = intent.getStringExtra(TelecomManager.EXTRA_NOTIFICATION_PHONE_NUMBER).orEmpty()
        val count = intent.getIntExtra(TelecomManager.EXTRA_NOTIFICATION_COUNT, 0)
        if (count <= 0) return
        Log.i(TAG, "Telecom delegated a missed-call notification (count=$count)")

        IncomingCallNotifier.notifyMissedCallFromTelecom(
            context = context,
            number = number,
            titleRes = R.string.notification_missed_call_title,
        )
    }

    companion object {
        private const val TAG = "MissedCallNotification"

        /**
         * Both spellings on purpose. `TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION` is
         * `android.telecom.action.…` and `TelephonyManager`'s is `android.telephony.action.…`;
         * AOSP has sent this broadcast under both over time, and the whole benefit of this
         * receiver depends on Telecom's `queryBroadcastReceivers` matching the filter. Declaring
         * one and guessing wrong buys nothing and fails silently.
         */
        val HANDLED_ACTIONS =
            setOf(
                "android.telecom.action.SHOW_MISSED_CALLS_NOTIFICATION",
                "android.telephony.action.SHOW_MISSED_CALLS_NOTIFICATION",
            )
    }
}
