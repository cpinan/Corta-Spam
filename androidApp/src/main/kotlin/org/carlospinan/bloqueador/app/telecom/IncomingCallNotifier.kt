package org.carlospinan.bloqueador.app.telecom

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import org.carlospinan.bloqueador.app.MainActivity
import org.carlospinan.bloqueador.app.R
import org.carlospinan.bloqueador.app.ShowCallLogIntent

/**
 * Posts the full-screen-intent notification Telecom's default-dialer contract expects us to
 * supply ourselves: [PassthroughInCallService.onCallAdded]'s plain `startActivity` call only
 * reaches the screen when the app is already foregrounded -- Android silently drops
 * background-activity-launch attempts when the screen is off/locked, which is the common case
 * for a real incoming call. A notification with `setFullScreenIntent` is the OS-sanctioned way
 * to launch UI over the lock screen instead.
 */
object IncomingCallNotifier {
    private const val CHANNEL_ID = "incoming_calls"
    private const val NOTIFICATION_ID = 1001
    private const val ONGOING_CHANNEL_ID = "ongoing_call"
    private const val ONGOING_NOTIFICATION_ID = 1002
    private const val HISTORY_CHANNEL_ID = "call_history"
    private const val HISTORY_NOTIFICATION_ID_BASE = 2_000_000
    private const val HISTORY_ID_RANGE = 1_000_000L
    private const val TAG = "IncomingCallNotifier"

    internal val repeatedCalls = RepeatedCallNotifications()

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_incoming_calls_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notification_channel_incoming_calls_desc)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        manager.createNotificationChannel(channel)

        val ongoingChannel =
            NotificationChannel(
                ONGOING_CHANNEL_ID,
                context.getString(R.string.notification_channel_ongoing_call_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_ongoing_call_desc)
            }
        manager.createNotificationChannel(ongoingChannel)

        val historyChannel =
            NotificationChannel(
                HISTORY_CHANNEL_ID,
                context.getString(R.string.notification_channel_history_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_history_desc)
            }
        manager.createNotificationChannel(historyChannel)
    }

    fun notifyIncomingCall(
        context: Context,
        number: String,
    ) {
        if (!canPostNotifications(context)) return

        val displayName =
            ContactNameLookup.displayNameFor(context, number)
                ?: number.ifBlank { context.getString(R.string.notification_unknown_caller) }

        val fullScreenPendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val answerPendingIntent = actionPendingIntent(context, CallActionReceiver.ACTION_ANSWER, requestCode = 1)
        val declinePendingIntent = actionPendingIntent(context, CallActionReceiver.ACTION_DECLINE, requestCode = 2)
        val caller = Person.Builder().setName(displayName).build()

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle(displayName)
                .setContentText(context.getString(R.string.notification_incoming_call))
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, declinePendingIntent, answerPendingIntent))
                .build()

        post(context, NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /**
     * Low-priority "return to call" notification while a call is active. [InCallActivity] is
     * `excludeFromRecents` and finishes once dismissed, so without this there's no way back to
     * a live call after leaving it (locking the screen, hitting home, etc).
     */
    fun notifyOngoingCall(
        context: Context,
        number: String,
    ) {
        if (!canPostNotifications(context)) return

        val displayName =
            ContactNameLookup.displayNameFor(context, number)
                ?: number.ifBlank { context.getString(R.string.notification_unknown_caller) }
        val contentPendingIntent =
            PendingIntent.getActivity(
                context,
                1,
                Intent(context, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val hangUpPendingIntent = actionPendingIntent(context, CallActionReceiver.ACTION_HANG_UP, requestCode = 3)
        val caller = Person.Builder().setName(displayName).build()

        val notification =
            NotificationCompat
                .Builder(context, ONGOING_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle(displayName)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .setStyle(NotificationCompat.CallStyle.forOngoingCall(caller, hangUpPendingIntent))
                .build()

        post(context, ONGOING_NOTIFICATION_ID, notification)
    }

    fun cancelOngoing(context: Context) {
        NotificationManagerCompat.from(context).cancel(ONGOING_NOTIFICATION_ID)
    }

    /**
     * Posted after the call already ended -- missed, or blocked (with why). Not full-screen.
     *
     * [actions] adds one-tap rule buttons. They live here rather than on the ringing notification
     * because [NotificationCompat.CallStyle] fixes that one's buttons to answer/decline and
     * ignores anything added alongside -- and because deciding a number's fate mid-ring is the
     * wrong moment anyway.
     */
    fun notifyCallResult(
        context: Context,
        number: String,
        @StringRes titleRes: Int,
        reason: String?,
        actions: Set<CallResultAction> = emptySet(),
    ) {
        if (!canPostNotifications(context)) return

        val displayName =
            ContactNameLookup.displayNameFor(context, number)
                ?: number.ifBlank { context.getString(R.string.notification_unknown_caller) }
        val text =
            if (reason != null) {
                context.getString(R.string.notification_call_result_with_reason, displayName, reason)
            } else {
                displayName
            }

        // A blank number cannot be told apart from the next blank one, so it is never counted --
        // otherwise every withheld-number call would pile onto the same growing tally.
        val bucket = if (number.isBlank()) null else repeatedCalls.record(number)
        val notificationNumber = bucket?.representative ?: number

        val builder =
            NotificationCompat
                .Builder(context, HISTORY_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_call_missed)
                .setContentTitle(context.getString(titleRes))
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(callLogPendingIntent(context, notificationNumber))

        // Posting to the same id already replaced the previous notification; what was missing was
        // any sign that it had happened, so five calls from one spammer looked like one call.
        if (bucket != null && bucket.count > 1) {
            builder.setSubText(
                context.resources.getQuantityString(
                    R.plurals.notification_call_attempts,
                    bucket.count,
                    bucket.count,
                ),
            )
        }

        if (number.isNotBlank()) {
            actions.forEach { action ->
                builder.addAction(
                    action.iconRes,
                    context.getString(action.labelRes),
                    ruleActionPendingIntent(context, action, notificationNumber),
                )
            }
        }

        post(context, historyNotificationId(notificationNumber), builder.build())
    }

    /**
     * One-tap rule buttons on a finished call's notification.
     *
     * Which of them a given notification carries is decided per call outcome, not offered as a
     * pair everywhere: "Block" on a call that was already blocked does nothing, and "Allow" on a
     * call that already came through does nothing either. A button that changes nothing when
     * tapped teaches the user their taps are ignored.
     */
    enum class CallResultAction(
        @param:StringRes val labelRes: Int,
        val iconRes: Int,
        private val broadcastAction: String,
    ) {
        BLOCK(
            R.string.notification_action_block,
            android.R.drawable.ic_menu_close_clear_cancel,
            CallActionReceiver.ACTION_BLOCK_NUMBER,
        ),
        ALLOWLIST(
            R.string.notification_action_allowlist,
            android.R.drawable.ic_menu_add,
            CallActionReceiver.ACTION_ALLOW_NUMBER,
        ),

        /**
         * Returns the call. Offered on the outcomes where the user lost one -- missed, and a
         * repeat caller that got through -- and deliberately not on a blocked call, where the
         * whole point of the rule was that this caller should not be reached.
         */
        CALL_BACK(
            R.string.notification_action_call_back,
            android.R.drawable.sym_action_call,
            CallActionReceiver.ACTION_CALL_BACK,
        ),
        ;

        internal fun intentAction(): String = broadcastAction
    }

    /**
     * Opens the call log on this caller when the notification body is tapped.
     *
     * A withheld number gets the app's normal launch intent instead: there is nothing to look up,
     * and a tap that opens the log filtered to the empty string would show an empty screen. The
     * request code is the notification's own id, so each caller keeps its own PendingIntent --
     * see [ruleActionPendingIntent] for what happens when two of them collide.
     */
    private fun callLogPendingIntent(
        context: Context,
        number: String,
    ): PendingIntent {
        val intent =
            if (number.isBlank()) {
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                ShowCallLogIntent.forNumber(context, number)
            }
        return PendingIntent.getActivity(
            context,
            historyNotificationId(number),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * A PendingIntent that survives another number's notification being posted next to it.
     *
     * PendingIntent equality ignores extras, so two of these built with the same request code and
     * the same action would be the *same* object -- and `FLAG_UPDATE_CURRENT` would rewrite the
     * first notification's number to the second's. Blocking one caller would then blocklist a
     * different one. The per-number `data` URI is what keeps them distinct.
     */
    private fun ruleActionPendingIntent(
        context: Context,
        action: CallResultAction,
        number: String,
    ): PendingIntent {
        val intent =
            Intent(context, CallActionReceiver::class.java)
                .setAction(action.intentAction())
                .setData("cortaspam://call/${Uri.encode(number)}".toUri())
                .putExtra(CallActionReceiver.EXTRA_NUMBER, number)
        return PendingIntent.getBroadcast(
            context,
            historyNotificationId(number),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Dismisses the finished-call notification for [number], once its button has been acted on. */
    fun cancelCallResult(
        context: Context,
        number: String,
    ) {
        NotificationManagerCompat.from(context).cancel(historyNotificationId(repeatedCalls.representativeFor(number)))
        // The user has dealt with this caller, so the next call from them starts a fresh count
        // rather than resuming a tally they already acted on.
        repeatedCalls.clear(number)
    }

    /**
     * A stable id per number, guaranteed to stay inside the history block.
     *
     * This was `HISTORY_NOTIFICATION_ID_BASE + number.hashCode()`, which for a negative hash
     * lands anywhere in Int -- including on the ringing-call id (1001) and the ongoing-call id
     * (1002), where posting a "missed call" would have replaced the live call's notification.
     */
    internal fun historyNotificationId(number: String): Int =
        HISTORY_NOTIFICATION_ID_BASE + (number.hashCode().toLong().let { if (it < 0) -it else it } % HISTORY_ID_RANGE).toInt()

    /**
     * Posts [notification], tolerating the permission being gone.
     *
     * Every caller checks [canPostNotifications] first, but that check and this call are two
     * separate moments: the user can revoke notifications from the shade in between, and on the
     * ringing-call path a `SecurityException` here would take down the service handling a live
     * call. Lint can't see through the helper either, hence the suppression — the check it asks
     * for is genuinely present, and the exception it warns about is genuinely handled.
     */
    @SuppressLint("MissingPermission")
    private fun post(
        context: Context,
        id: Int,
        notification: Notification,
    ) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission was revoked before the notification could be posted", e)
        }
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun actionPendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, CallActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
