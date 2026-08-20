package org.carlospinan.bloqueador.app.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CountriesTest {
    @Test
    fun everyDiallingCodeAppearsExactlyOnce() {
        val duplicates =
            COUNTRIES
                .groupBy { it.code }
                .filterValues { it.size > 1 }
                .mapValues { (_, entries) -> entries.map { it.name } }

        // CountryRule is UNIQUE(country_code) and inserted with INSERT OR IGNORE, so a second
        // entry sharing a code can never be added: it shows up in the picker, the user taps it,
        // and nothing happens. Codes covering several territories get one combined name.
        assertEquals(emptyMap(), duplicates, "duplicate dialling codes are unaddable")
    }

    @Test
    fun everyEntryIsUsable() {
        for (country in COUNTRIES) {
            assertTrue(country.code.isNotBlank(), "blank code for ${country.name}")
            assertTrue(country.code.all { it.isDigit() }, "non-digit code ${country.code}")
            assertTrue(country.name.isNotBlank(), "blank name for +${country.code}")
        }
    }

    @Test
    fun sharedCodesNameEveryTerritoryTheyCover() {
        fun nameFor(code: String) = COUNTRIES.single { it.code == code }.name

        assertEquals("United States / Canada", nameFor("1"))
        assertEquals("Russia / Kazakhstan", nameFor("7"))
        assertEquals("Morocco / Western Sahara", nameFor("212"))
        assertEquals("Guadeloupe / Saint Martin", nameFor("590"))
    }

    /**
     * Every entry names at least one ISO 3166-1 alpha-2 region.
     *
     * The region is what makes the name translatable — it is the key the platform turns into a
     * name in the user's language — so an entry without one silently falls back to English
     * forever, which is the bug this data exists to fix.
     */
    @Test
    fun everyEntryNamesItsRegions() {
        for (country in COUNTRIES) {
            assertTrue(country.regions.isNotEmpty(), "no region for +${country.code} ${country.name}")
            for (region in country.regions) {
                assertEquals(2, region.length, "not an alpha-2 region: '$region' for +${country.code}")
                assertTrue(
                    region.all { it in 'A'..'Z' },
                    "region must be upper-case A-Z: '$region' for +${country.code}",
                )
            }
        }
    }

    /** A code covering several territories keeps them all, or the combined name loses one. */
    @Test
    fun sharedDiallingCodesKeepEveryTerritory() {
        val shared = COUNTRIES.filter { it.name.contains(" / ") }
        assertTrue(shared.isNotEmpty(), "expected some combined names to exist")
        for (country in shared) {
            assertEquals(
                country.name.split(" / ").size,
                country.regions.size,
                "+${country.code} names ${country.name} but lists ${country.regions}",
            )
        }
    }
}
