package org.carlospinan.bloqueador.app.rules

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmergencyNumbersTest {
    @Test
    fun `the numbers reachable on any GSM handset are recognised`() {
        assertTrue(EmergencyNumbers.isWellKnown("112"))
        assertTrue(EmergencyNumbers.isWellKnown("911"))
    }

    /** The app ships in four languages; the countries behind them are the ones covered. */
    @Test
    fun `the shipped locales' own emergency numbers are recognised`() {
        assertTrue(EmergencyNumbers.isWellKnown("999")) // en-GB
        assertTrue(EmergencyNumbers.isWellKnown("105")) // Peru, es
        assertTrue(EmergencyNumbers.isWellKnown("190")) // Brazil, pt
        assertTrue(EmergencyNumbers.isWellKnown("101")) // India, hi
    }

    @Test
    fun `spacing and punctuation a dialler inserts do not matter`() {
        assertTrue(EmergencyNumbers.isWellKnown(" 1 1 2 "))
        assertTrue(EmergencyNumbers.isWellKnown("9-1-1"))
    }

    /**
     * Whole-number matching, not a prefix. Getting this wrong would hand every caller whose number
     * starts with 112 a free pass through the blocklist.
     */
    @Test
    fun `an ordinary number that merely starts with one is not an emergency number`() {
        assertFalse(EmergencyNumbers.isWellKnown("1120000"))
        assertFalse(EmergencyNumbers.isWellKnown("9110000"))
    }

    /** An emergency number is dialled bare; `+34112` is an ordinary international number. */
    @Test
    fun `an international number ending in one is not an emergency number`() {
        assertFalse(EmergencyNumbers.isWellKnown("+34112"))
        assertFalse(EmergencyNumbers.isWellKnown("+112"))
    }

    @Test
    fun `nothing typed is not an emergency number`() {
        assertFalse(EmergencyNumbers.isWellKnown(""))
        assertFalse(EmergencyNumbers.isWellKnown("   "))
    }
}
