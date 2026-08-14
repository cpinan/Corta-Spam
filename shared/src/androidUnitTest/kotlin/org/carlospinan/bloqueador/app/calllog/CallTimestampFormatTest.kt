package org.carlospinan.bloqueador.app.calllog

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The call log's dates were assembled by hand from `Month.name`, an English enum constant, so a
 * fully translated screen printed "Aug 14, 2026" to Spanish, Portuguese and Hindi readers. It was
 * invisible to every existing test — the English output was correct — and was found by
 * screenshotting the app in Spanish for the store listing.
 *
 * These assertions are about the *locale being honoured*, not about exact wording: CLDR changes
 * month abbreviations between releases, and pinning "14 ago 2026" would fail on a JDK upgrade for
 * no useful reason.
 */
class CallTimestampFormatTest {
    private val original = Locale.getDefault()

    // 2026-08-14T15:18:00Z, the timestamp on the screenshot that exposed this.
    private val timestamp = 1_786_641_480_000L

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(original)
    }

    @Test
    fun `a Spanish reader gets no English month`() {
        Locale.setDefault(Locale.forLanguageTag("es-419"))

        val formatted = formatCallTimestamp(timestamp)

        assertFalse(
            ENGLISH_MONTHS.any { formatted.contains(it) },
            "Spanish call log still shows an English month: $formatted",
        )
        assertTrue(formatted.contains("2026"), "expected the year, got $formatted")
    }

    @Test
    fun `an English reader still gets an English month`() {
        Locale.setDefault(Locale.US)

        val formatted = formatCallTimestamp(timestamp)

        assertTrue(formatted.contains("Aug"), "expected an English month, got $formatted")
    }

    /** Two locales must not format the same instant identically, or nothing is being localized. */
    @Test
    fun `the same instant reads differently in different locales`() {
        Locale.setDefault(Locale.US)
        val english = formatCallTimestamp(timestamp)
        Locale.setDefault(Locale.forLanguageTag("es-419"))
        val spanish = formatCallTimestamp(timestamp)

        assertTrue(english != spanish, "both locales produced \"$english\"")
    }

    /**
     * The locale is read per call, not captured once: Android's per-app language setting changes
     * it inside a running process, and a formatter cached at class-init would keep printing the
     * language the user just left.
     */
    @Test
    fun `a locale change during the process is picked up`() {
        Locale.setDefault(Locale.US)
        val before = formatCallTimestamp(timestamp)
        Locale.setDefault(Locale.forLanguageTag("pt-BR"))
        val after = formatCallTimestamp(timestamp)

        assertTrue(before != after, "the formatter kept the locale it was first called with")
    }

    private companion object {
        val ENGLISH_MONTHS =
            listOf("Jan", "Feb", "Mar", "Apr", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
}
