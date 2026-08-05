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
    fun theDoubleZeroAccessCodeCountsAsInternational() {
        assertEquals("34", PhoneNumberParser.parseCountryCode("0034600123456"))
        assertEquals("1264", PhoneNumberParser.parseCountryCode("0012645551234"))
    }

    @Test
    fun formattingCharactersAreIgnored() {
        assertEquals("34", PhoneNumberParser.parseCountryCode("+34 600 123 456"))
        assertEquals("1", PhoneNumberParser.parseCountryCode("+1 (212) 555-1234"))
    }

    @Test
    fun aNationalFormatNumberHasNoCountry() {
        // The bug this replaces: normalizing stripped the "+", so a national number was read as
        // international and attributed to whatever country its leading digits happened to spell.
        // Blocking Morocco (+212) then blocked every Manhattan (212) number.
        assertNull(PhoneNumberParser.parseCountryCode("2125551234"))
        // Madrid landline; "91" is India's country code.
        assertNull(PhoneNumberParser.parseCountryCode("912345678"))
        assertNull(PhoneNumberParser.parseCountryCode("34600123456"))
    }

    @Test
    fun toE164CanonicalisesBothInternationalSpellings() {
        assertEquals("+34600123456", PhoneNumberParser.toE164OrNull("+34600123456"))
        assertEquals("+34600123456", PhoneNumberParser.toE164OrNull("0034600123456"))
        assertEquals("+34600123456", PhoneNumberParser.toE164OrNull("  +34 600-123-456  "))
    }

    @Test
    fun toE164RejectsAnythingNotWrittenInternationally() {
        assertNull(PhoneNumberParser.toE164OrNull("2125551234"))
        assertNull(PhoneNumberParser.toE164OrNull(""))
        assertNull(PhoneNumberParser.toE164OrNull("+"))
        assertNull(PhoneNumberParser.toE164OrNull("abc"))
        // 16 digits, one past what E.164 allows.
        assertNull(PhoneNumberParser.toE164OrNull("+1234567890123456"))
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
