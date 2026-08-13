package org.carlospinan.bloqueador.app.contacts

import org.carlospinan.bloqueador.app.rules.PhoneNumberParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The map under test is built the way [ContactsGateway.contactNames] builds it — keyed by every
 * comparison form of the saved number, not by its digits alone. A test that keys it by hand
 * would pass against a lookup that cannot work on a device.
 */
private fun contactsOf(vararg saved: Pair<String, String>): Map<String, String> {
    val names = mutableMapOf<String, String>()
    saved.forEach { (number, name) ->
        PhoneNumberParser.comparisonKeys(number).forEach { key -> names.getOrPut(key) { name } }
    }
    return names
}

class ContactDisplayNameTest {
    @Test
    fun `names a contact saved nationally when the call arrives in E164`() {
        // The common case, and the one single-key lookup gets wrong: the card holds no country
        // code, the call states one, so their digit strings never match.
        val contacts = contactsOf("611 99 88 77" to "Ana")

        assertEquals("Ana", contactDisplayName("+34611998877", contacts))
    }

    @Test
    fun `names a contact saved with a trunk zero when the call arrives in E164`() {
        val contacts = contactsOf("07700900123" to "Bob")

        assertEquals("Bob", contactDisplayName("+447700900123", contacts))
    }

    @Test
    fun `names a contact saved in E164 when the call arrives in E164`() {
        val contacts = contactsOf("+34611998877" to "Ana")

        assertEquals("Ana", contactDisplayName("+34611998877", contacts))
    }

    @Test
    fun `falls back to the number when nobody in the address book claims it`() {
        val contacts = contactsOf("611 99 88 77" to "Ana")

        assertEquals("+34600000000", contactDisplayName("+34600000000", contacts))
    }

    @Test
    fun `falls back to the number when the address book is empty`() {
        assertEquals("+34611998877", contactDisplayName("+34611998877", emptyMap()))
    }

    @Test
    fun `recognises a contact saved nationally when the call arrives in E164`() {
        // The case ContactsContract.PhoneLookup gets wrong, which silenced a real contact's
        // missed-call notification on a device whose region did not match the number.
        val contacts = contactsOf("611 99 88 77" to "Ana")

        assertTrue(isKnownContact("+34611998877", contacts))
    }

    @Test
    fun `does not recognise a number the address book has never seen`() {
        val contacts = contactsOf("611 99 88 77" to "Ana")

        assertFalse(isKnownContact("+34600000000", contacts))
    }

    @Test
    fun `recognises nobody when the address book is empty`() {
        assertFalse(isKnownContact("+34611998877", emptyMap()))
    }
}
