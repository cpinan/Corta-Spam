package org.carlospinan.bloqueador.app

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class ShowCallLogIntentTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `the number survives the round trip through a real intent`() {
        val intent = ShowCallLogIntent.forNumber(context, "+34611998877")

        assertEquals("+34611998877", ShowCallLogIntent.numberFrom(intent))
    }

    @Test
    fun `another action carries no call-log number`() {
        assertNull(ShowCallLogIntent.numberFrom(android.content.Intent.ACTION_MAIN, "+34611998877"))
    }

    @Test
    fun `a blank number is not a request`() {
        assertNull(ShowCallLogIntent.numberFrom(ShowCallLogIntent.ACTION, "   "))
        assertNull(ShowCallLogIntent.numberFrom(ShowCallLogIntent.ACTION, null))
    }

    /**
     * PendingIntent equality ignores extras, so two notifications built from intents that differ
     * only by their number would share one PendingIntent -- and the older notification would be
     * repointed at the newer caller. The per-number data URI is what prevents it, and
     * `filterEquals` is the same comparison PendingIntent makes.
     */
    @Test
    fun `two callers produce intents that are not interchangeable`() {
        val first = ShowCallLogIntent.forNumber(context, "+34611998877")
        val second = ShowCallLogIntent.forNumber(context, "+34600111222")

        assertNotEquals(first.data, second.data)
        assertEquals(false, first.filterEquals(second))
    }
}
