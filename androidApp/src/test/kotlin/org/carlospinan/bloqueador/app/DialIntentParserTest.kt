package org.carlospinan.bloqueador.app

import android.app.Application
import android.content.Intent
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `application = Application::class` for the reason `ContactNameLookupTest` records: the real
 * `CortaSpamApp.onCreate()` calls `startKoin()`, whose global context outlives the per-test
 * Application, so every test after the first dies with `KoinApplicationAlreadyStartedException`.
 * Nothing here needs the DI graph — only Android's `Uri` parsing.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
class DialIntentParserTest {
    @Test
    fun `reads the number from an ACTION_DIAL tel intent`() {
        assertEquals("+34600123456", DialIntentParser.numberFrom(Intent.ACTION_DIAL, "tel:+34600123456".toUri()))
    }

    @Test
    fun `reads the number from an ACTION_VIEW tel intent`() {
        assertEquals("+34600123456", DialIntentParser.numberFrom(Intent.ACTION_VIEW, "tel:+34600123456".toUri()))
    }

    /**
     * A '#' in a dialled number arrives percent-encoded. Reading `Uri.fragment`-style would treat
     * everything after it as a fragment and hand back a truncated number.
     */
    @Test
    fun `decodes a percent-encoded hash`() {
        assertEquals("*21#", DialIntentParser.numberFrom(Intent.ACTION_DIAL, "tel:*21%23".toUri()))
    }

    @Test
    fun `keeps a national number as typed`() {
        assertEquals("611 99 88 77", DialIntentParser.numberFrom(Intent.ACTION_DIAL, "tel:611 99 88 77".toUri()))
    }

    @Test
    fun `ACTION_DIAL with no data means open the dialer, not dial nothing`() {
        assertNull(DialIntentParser.numberFrom(Intent.ACTION_DIAL, null))
    }

    @Test
    fun `ignores a non-tel scheme`() {
        assertNull(DialIntentParser.numberFrom(Intent.ACTION_VIEW, "https://example.com".toUri()))
    }

    @Test
    fun `ignores an unrelated action`() {
        assertNull(DialIntentParser.numberFrom(Intent.ACTION_MAIN, "tel:+34600123456".toUri()))
        assertNull(DialIntentParser.numberFrom(null, "tel:+34600123456".toUri()))
    }

    @Test
    fun `ignores a tel uri with an empty number`() {
        assertNull(DialIntentParser.numberFrom(Intent.ACTION_DIAL, "tel:".toUri()))
    }

    @Test
    fun `accepts the scheme in any case`() {
        assertEquals("+34600123456", DialIntentParser.numberFrom(Intent.ACTION_DIAL, "TEL:+34600123456".toUri()))
    }
}
