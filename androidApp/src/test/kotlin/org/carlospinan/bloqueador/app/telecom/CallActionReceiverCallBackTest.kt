package org.carlospinan.bloqueador.app.telecom

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
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
 * The call-back button on a finished call's notification — the one path in this app that places a
 * call from outside a Composable.
 *
 * Only the outgoing-intent decision is exercised here. Whether the call connects is Telecom's
 * business and a real handset's; what this can prove is that the button fires the right intent,
 * that a denied permission does not turn it into a no-op, and that it never hands the user back
 * to the dialer this app replaced.
 *
 * `application = Application::class` keeps `CortaSpamApp.onCreate()`'s `startKoin()` out of the
 * JVM, as `ContactNameLookupTest` records. The receiver's `ruleRepository` is a lazy `inject()`
 * that this path never touches.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class CallActionReceiverCallBackTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val app get() = ApplicationProvider.getApplicationContext<Application>()

    private fun callBackIntent(number: String?): Intent =
        Intent(CallActionReceiver.ACTION_CALL_BACK).apply {
            if (number != null) putExtra(CallActionReceiver.EXTRA_NUMBER, number)
        }

    private fun receive(intent: Intent) = CallActionReceiver().onReceive(context, intent)

    @Before
    fun setUp() {
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.CALL_PHONE)
        context.getSystemService(NotificationManager::class.java).cancelAll()
        IncomingCallNotifier.repeatedCalls.reset()
        // Drain anything an earlier test queued, so `nextStartedActivity` is this test's.
        while (Shadows.shadowOf(app).nextStartedActivity != null) Unit
    }

    @Test
    fun `with CALL_PHONE granted the button places the call`() {
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.CALL_PHONE)

        receive(callBackIntent("+34600123456"))

        val started = Shadows.shadowOf(app).nextStartedActivity!!
        assertEquals(Intent.ACTION_CALL, started.action)
        assertEquals("tel:+34600123456", started.data.toString())
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    /**
     * A BroadcastReceiver cannot show a permission dialog, and firing ACTION_CALL without the
     * grant throws a SecurityException the user would read as a dead button. The fallback is this
     * app's own keypad, pre-filled.
     */
    @Test
    fun `without CALL_PHONE the button opens this app's keypad instead`() {
        receive(callBackIntent("+34600123456"))

        val started = Shadows.shadowOf(app).nextStartedActivity!!
        assertEquals(Intent.ACTION_DIAL, started.action)
        assertEquals("tel:+34600123456", started.data.toString())
        assertEquals(context.packageName, started.`package`)
    }

    /**
     * Pinning the fallback matters: an unpinned ACTION_DIAL is offered to every dialer installed,
     * including the one this app replaced — the trip to another app the keypad exists to remove.
     */
    @Test
    fun `the fallback is never offered to another dialer`() {
        receive(callBackIntent("+34600123456"))

        assertEquals(context.packageName, Shadows.shadowOf(app).nextStartedActivity!!.`package`)
    }

    @Test
    fun `the number is trimmed and encoded`() {
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.CALL_PHONE)

        receive(callBackIntent("  *21#  "))

        assertEquals(
            "tel:*21%23",
            Shadows
                .shadowOf(app)
                .nextStartedActivity!!
                .data
                .toString(),
        )
    }

    @Test
    fun `a missing number starts nothing`() {
        receive(callBackIntent(null))
        assertNull(Shadows.shadowOf(app).nextStartedActivity)

        receive(callBackIntent("   "))
        assertNull(Shadows.shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `calling back dismisses the notification it came from`() {
        IncomingCallNotifier.notifyCallResult(
            context,
            "+34600123456",
            R.string.notification_missed_call_title,
            reason = null,
            actions = setOf(IncomingCallNotifier.CallResultAction.CALL_BACK),
        )
        assertEquals(1, context.getSystemService(NotificationManager::class.java).activeNotifications.size)

        receive(callBackIntent("+34600123456"))

        assertEquals(0, context.getSystemService(NotificationManager::class.java).activeNotifications.size)
    }

    /** A returned call is a dealt-with caller, so the next one starts a fresh tally. */
    @Test
    fun `calling back restarts the attempt count`() {
        repeat(3) {
            IncomingCallNotifier.notifyCallResult(context, "+34600123456", R.string.notification_missed_call_title, null)
        }

        receive(callBackIntent("+34600123456"))

        assertEquals(1, IncomingCallNotifier.repeatedCalls.record("+34600123456").count)
    }
}
