package org.carlospinan.bloqueador.app.rules

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Country names must read in the user's language, not in the one the rule was created in.
 *
 * The bug: `CountryRule` persists the name it was made with, and so does the `rule_detail` on a
 * blocked call. The names came from [COUNTRIES], which is English, so a Spanish call log read
 * "País: Morocco / Western Sahara" — and no language change could ever have fixed it, because the
 * English string was already in the database. Seen in the Spanish store screenshot on 2026-08-20.
 *
 * These live in `androidUnitTest` rather than `commonTest` because they assert what the *platform*
 * returns, and `platformCountryName` only has a body per platform. The common side's own rules —
 * every entry has regions, a shared code keeps every territory — are in `CountriesTest`.
 */
class CountryNamesTest {
    private val original: Locale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(original)
    }

    @Test
    fun `a country reads in the language of the reader`() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("Germany", CountryNames.forDiallingCode("49"))

        Locale.setDefault(Locale("es"))
        assertEquals("Alemania", CountryNames.forDiallingCode("49"))
    }

    /**
     * The case from the screenshot. A shared dialling code joins its territories, and every part
     * is translated -- not just the first.
     */
    @Test
    fun `a shared dialling code translates every territory it covers`() {
        Locale.setDefault(Locale("es"))

        val name = CountryNames.forDiallingCode("212", storedName = "Morocco / Western Sahara")

        assertEquals("Marruecos / Sáhara Occidental", name)
        assertFalse(name.contains("Morocco"), "the stored English name leaked through")
    }

    /**
     * The stored name is a fallback, and must lose to a name the platform can produce.
     *
     * Without this, a row written by an older build would pin its language forever -- which is the
     * original defect wearing a different hat.
     */
    @Test
    fun `the stored name never wins over a translated one`() {
        Locale.setDefault(Locale("es"))

        assertEquals("Francia", CountryNames.forDiallingCode("33", storedName = "France"))
    }

    /** A code this build has never heard of still shows what the database recorded. */
    @Test
    fun `an unknown dialling code falls back to the stored name`() {
        assertEquals("Atlantis", CountryNames.forDiallingCode("99999", storedName = "Atlantis"))
    }

    /** And with nothing recorded either, the code itself beats an empty row. */
    @Test
    fun `an unknown dialling code with nothing stored shows the code`() {
        assertEquals("99999", CountryNames.forDiallingCode("99999"))
    }

    /**
     * Every entry resolves to a real name, in every locale the app ships.
     *
     * This is the check that catches a mistyped region: "XX" compiles, passes `CountriesTest`'s
     * alpha-2 shape check, and then renders as the English fallback for that one country in every
     * language — invisible unless somebody reads all 223 rows in Spanish.
     */
    @Test
    fun `every entry has a platform name in every shipped locale`() {
        val unresolved = mutableListOf<String>()
        for (tag in listOf("en", "es", "pt", "hi")) {
            Locale.setDefault(Locale(tag))
            for (country in COUNTRIES) {
                for (region in country.regions) {
                    if (platformCountryName(region) == null) {
                        unresolved += "$tag: +${country.code} ${country.name} -> $region"
                    }
                }
            }
        }
        assertEquals(emptyList(), unresolved, "regions the platform cannot name")
    }

    /** Searching by the English name has to keep working for a reader who is not using English. */
    @Test
    fun `the picker matches the English name, the translated name and the code`() {
        Locale.setDefault(Locale("es"))
        val germany = COUNTRIES.first { it.code == "49" }

        assertTrue(CountryNames.matches(germany, "Alemania"), "translated name")
        assertTrue(CountryNames.matches(germany, "Germany"), "English name")
        assertTrue(CountryNames.matches(germany, "49"), "dialling code")
        assertFalse(CountryNames.matches(germany, "Francia"))
    }
}
