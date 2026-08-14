package org.carlospinan.bloqueador.app.contacts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContactSearchTest {
    private val ana = Contact(name = "Ana Torres", number = "+34 611 99 88 77")
    private val juana = Contact(name = "Juana Ruiz", number = "600 111 222")
    private val bank = Contact(name = "Banco BBVA", number = "911234567")

    private val book = listOf(ana, bank, juana)

    @Test
    fun `an empty query matches nothing`() {
        assertTrue(ContactSearch.match(book, "").isEmpty())
        assertTrue(ContactSearch.match(book, "   ").isEmpty())
    }

    @Test
    fun `a name prefix matches, ignoring case`() {
        assertEquals(listOf(ana), ContactSearch.match(book, "ana t"))
        assertEquals(listOf(ana), ContactSearch.match(book, "ANA T"))
    }

    /** The stored formatting is not what the user types, and must not decide whether they find it. */
    @Test
    fun `digits match a number saved with spaces and a country code`() {
        assertEquals(listOf(ana), ContactSearch.match(book, "61199"))
    }

    @Test
    fun `a typed country code matches the same contact`() {
        assertEquals(listOf(ana), ContactSearch.match(book, "+34 611"))
    }

    /**
     * The national number is what people know by heart; the address book is what stores the
     * country code. A search that only matched from the front would never find this contact.
     */
    @Test
    fun `the national part of an international number is found`() {
        assertEquals(listOf(ana), ContactSearch.match(book, "611998877"))
    }

    /** "ana" is a surname prefix in "Juana"? No -- and that is the point. */
    @Test
    fun `a syllable inside a word is not a match`() {
        assertEquals(listOf(ana), ContactSearch.match(book, "Ana"))
    }

    @Test
    fun `a second word in the name matches`() {
        assertEquals(listOf(ana), ContactSearch.match(book, "torres"))
    }

    @Test
    fun `a name prefix outranks a number that merely contains the digits`() {
        val digitsInName = Contact(name = "911 Emergencies", number = "+34 700 000 000")
        val matches = ContactSearch.match(listOf(bank, digitsInName), "911")
        assertEquals(listOf(digitsInName, bank), matches)
    }

    @Test
    fun `equal-ranked matches keep the order they came in`() {
        val ana2 = Contact(name = "Ana Belen", number = "622 333 444")
        val matches = ContactSearch.match(listOf(ana, ana2), "ana")
        assertEquals(listOf(ana, ana2), matches)
    }

    @Test
    fun `nothing matches an unknown query`() {
        assertTrue(ContactSearch.match(book, "zzz").isEmpty())
    }
}
