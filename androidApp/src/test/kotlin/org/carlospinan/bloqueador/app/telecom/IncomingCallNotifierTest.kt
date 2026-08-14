package org.carlospinan.bloqueador.app.telecom

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.carlospinan.bloqueador.app.MainActivity
import org.carlospinan.bloqueador.app.R
import org.carlospinan.bloqueador.app.ShowCallLogIntent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The "replace, don't stack" behaviour asserted through the real notifier rather than through
 * [RepeatedCallNotifications] alone: a correct counter whose number never reaches a notification
 * is the shape of inert feature this codebase has shipped before.
 *
 * `application = Application::class` keeps `CortaSpamApp.onCreate()`'s `startKoin()` out of the
 * JVM — see `ContactNameLookupTest` for what happens otherwise.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class IncomingCallNotifierTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val manager get() = context.getSystemService(NotificationManager::class.java)

    private fun activeFor(number: String): Notification? =
        manager.activeNotifications
            .firstOrNull { it.id == IncomingCallNotifier.historyNotificationId(number) }
            ?.notification

    private fun subTextOf(notification: Notification): String? =
        notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

    @Before
    fun setUp() {
        Shadows
            .shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        manager.cancelAll()
        IncomingCallNotifier.repeatedCalls.reset()
    }

    @Test
    fun `a first blocked call posts one notification with no attempt count`() {
        IncomingCallNotifier.notifyCallResult(
            context,
            "+34600123456",
            R.string.notification_blocked_call_title,
            reason = null,
        )

        assertEquals(1, manager.activeNotifications.size)
        assertNull(subTextOf(activeFor("+34600123456")!!))
    }

    @Test
    fun `a second call from the same number replaces rather than stacks`() {
        repeat(2) {
            IncomingCallNotifier.notifyCallResult(
                context,
                "+34600123456",
                R.string.notification_blocked_call_title,
                reason = null,
            )
        }

        assertEquals(1, manager.activeNotifications.size)
        assertEquals("2 attempts", subTextOf(activeFor("+34600123456")!!))
    }

    /** Five calls from one spammer used to read as one call, five times over. */
    @Test
    fun `the attempt count keeps climbing`() {
        repeat(5) {
            IncomingCallNotifier.notifyCallResult(
                context,
                "+34600123456",
                R.string.notification_blocked_call_title,
                reason = null,
            )
        }

        assertEquals(1, manager.activeNotifications.size)
        assertEquals("5 attempts", subTextOf(activeFor("+34600123456")!!))
    }

    @Test
    fun `two different callers keep their own notifications`() {
        IncomingCallNotifier.notifyCallResult(context, "+34600123456", R.string.notification_blocked_call_title, null)
        IncomingCallNotifier.notifyCallResult(context, "+34600999999", R.string.notification_missed_call_title, null)

        assertEquals(2, manager.activeNotifications.size)
    }

    /** The same caller in two formats is one caller, and so is one notification. */
    @Test
    fun `a national and an international form share one notification`() {
        IncomingCallNotifier.notifyCallResult(context, "+34611998877", R.string.notification_blocked_call_title, null)
        IncomingCallNotifier.notifyCallResult(context, "611 99 88 77", R.string.notification_blocked_call_title, null)

        assertEquals(1, manager.activeNotifications.size)
        assertEquals("2 attempts", subTextOf(activeFor("+34611998877")!!))
    }

    @Test
    fun `acting on a notification dismisses it and restarts the count`() {
        repeat(3) {
            IncomingCallNotifier.notifyCallResult(context, "+34600123456", R.string.notification_blocked_call_title, null)
        }
        IncomingCallNotifier.cancelCallResult(context, "+34600123456")
        assertEquals(0, manager.activeNotifications.size)

        IncomingCallNotifier.notifyCallResult(context, "+34600123456", R.string.notification_blocked_call_title, null)
        assertNull(subTextOf(activeFor("+34600123456")!!))
    }

    /**
     * Withheld numbers arrive blank and are indistinguishable from each other, so counting them
     * would grow one shared tally across unrelated callers.
     */
    @Test
    fun `a withheld number is never counted`() {
        repeat(3) {
            IncomingCallNotifier.notifyCallResult(context, "", R.string.notification_missed_call_title, null)
        }

        assertEquals(1, manager.activeNotifications.size)
        assertNull(subTextOf(manager.activeNotifications.first().notification))
    }

    @Test
    fun `a missed call offers call back`() {
        IncomingCallNotifier.notifyCallResult(
            context,
            "+34600123456",
            R.string.notification_missed_call_title,
            reason = null,
            actions =
                setOf(
                    IncomingCallNotifier.CallResultAction.CALL_BACK,
                    IncomingCallNotifier.CallResultAction.BLOCK,
                ),
        )

        val actions = activeFor("+34600123456")!!.actions.orEmpty().map { it.title.toString() }
        assertTrue("Call back" in actions, "expected a call-back action, got $actions")
    }

    /**
     * The body of a finished-call notification used to do nothing at all -- no content intent, so
     * a tap dismissed the only surface reporting the call and left the log two screens away.
     */
    @Test
    fun `tapping a finished call opens the call log on that caller`() {
        IncomingCallNotifier.notifyCallResult(context, "+34600123456", R.string.notification_missed_call_title, null)

        val contentIntent = activeFor("+34600123456")!!.contentIntent
        assertNotNull(contentIntent, "the notification body has no content intent")
        val intent = Shadows.shadowOf(contentIntent).savedIntent
        assertEquals("+34600123456", ShowCallLogIntent.numberFrom(intent))
    }

    /**
     * Two callers must not share one PendingIntent: `FLAG_UPDATE_CURRENT` would rewrite the first
     * notification's number to the second's, and the tap would open the wrong caller.
     */
    @Test
    fun `each caller's notification opens its own number`() {
        IncomingCallNotifier.notifyCallResult(context, "+34600123456", R.string.notification_missed_call_title, null)
        IncomingCallNotifier.notifyCallResult(context, "+34600999999", R.string.notification_missed_call_title, null)

        val first = Shadows.shadowOf(activeFor("+34600123456")!!.contentIntent).savedIntent
        val second = Shadows.shadowOf(activeFor("+34600999999")!!.contentIntent).savedIntent
        assertEquals("+34600123456", ShowCallLogIntent.numberFrom(first))
        assertEquals("+34600999999", ShowCallLogIntent.numberFrom(second))
    }

    /** There is no caller to look up, so the tap opens the app rather than an empty search. */
    @Test
    fun `a withheld number's notification opens the app itself`() {
        IncomingCallNotifier.notifyCallResult(context, "", R.string.notification_missed_call_title, null)

        val contentIntent =
            manager.activeNotifications
                .first()
                .notification.contentIntent
        assertNotNull(contentIntent, "the notification body has no content intent")
        val intent = Shadows.shadowOf(contentIntent).savedIntent
        assertNull(ShowCallLogIntent.numberFrom(intent))
        assertEquals(MainActivity::class.java.name, intent.component?.className)
    }
}
