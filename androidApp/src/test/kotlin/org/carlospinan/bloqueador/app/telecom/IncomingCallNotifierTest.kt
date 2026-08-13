package org.carlospinan.bloqueador.app.telecom

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.carlospinan.bloqueador.app.R
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
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
}
