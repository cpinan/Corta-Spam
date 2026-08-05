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
}
