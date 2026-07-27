package org.carlospinan.bloqueador.app.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneNumberParserTest {
    @Test
    fun matchesSpain() {
        assertEquals("34", PhoneNumberParser.parseCountryCode("+34600123456"))
    }

    @Test
    fun matchesUkAndChina() {
        assertEquals("44", PhoneNumberParser.parseCountryCode("+441234567890"))
        assertEquals("86", PhoneNumberParser.parseCountryCode("+861234567890"))
    }

    @Test
    fun nanpMainlandFallsBackToBareCode() {
        assertEquals("1", PhoneNumberParser.parseCountryCode("+12125551234"))
    }

    @Test
    fun nanpCaribbeanPrefersLongestMatch() {
        assertEquals("1264", PhoneNumberParser.parseCountryCode("+12645551234"))
        assertEquals("1268", PhoneNumberParser.parseCountryCode("+12685551234"))
    }

    @Test
    fun noPlusPrefixStillMatches() {
        assertEquals("34", PhoneNumberParser.parseCountryCode("34600123456"))
    }

    @Test
    fun nonDigitInputReturnsNull() {
        assertNull(PhoneNumberParser.parseCountryCode("abc123"))
    }

    @Test
    fun emptyInputReturnsNull() {
        assertNull(PhoneNumberParser.parseCountryCode(""))
        assertNull(PhoneNumberParser.parseCountryCode("+"))
    }

    @Test
    fun tooShortToBePlausibleReturnsNull() {
        assertNull(PhoneNumberParser.parseCountryCode("123"))
    }

    @Test
    fun tooLongReturnsNull() {
        assertNull(PhoneNumberParser.parseCountryCode("+1234567890123456"))
    }
}
