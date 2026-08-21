package org.carlospinan.bloqueador.app.agenda

import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class AgendaFiltersTest {
    private val ana = Contact(name = "Ana Torres", number = "+34611998877", starred = true)
    private val bea = Contact(name = "Bea Ruiz", number = "600111222")
    private val carlos = Contact(name = "Carlos Pinan", number = "+51987654321")
    private val book = listOf(ana, bea, carlos)

    private fun blocked(number: String) = BlockedNumberEntry(id = 1, number = number, label = null, createdAt = 0)

    private fun allowed(number: String) = AllowlistedNumberEntry(id = 2, number = number, label = null, createdAt = 0)

    /**
     * The resting state of the screen is the whole address book. [ContactSearch] answers "what
     * matches what is being typed" and answers an empty query with nothing, which is right for the
     * keypad and would leave this screen blank until the user typed something.
     */
    @Test
    fun `an empty query keeps every contact`() {
        assertEquals(book, filterAgenda(book, AgendaFilter.ALL, query = ""))
    }

    @Test
    fun `the query narrows by name`() {
        assertEquals(listOf(bea), filterAgenda(book, AgendaFilter.ALL, query = "Bea"))
    }

    @Test
    fun `the query narrows by number`() {
        assertEquals(listOf(bea), filterAgenda(book, AgendaFilter.ALL, query = "600111"))
    }

    @Test
    fun `starred keeps only the platform favourites`() {
        assertEquals(listOf(ana), filterAgenda(book, AgendaFilter.STARRED, query = ""))
    }

    /**
     * The filter this screen exists for. The block list holds numbers, so it cannot answer
     * "which of the people in my phone have I blocked" -- this can.
     */
    @Test
    fun `blocked keeps the contacts a block rule matches`() {
        val result =
            filterAgenda(
                book,
                AgendaFilter.BLOCKED,
                query = "",
                blockedNumbers = listOf(blocked("+34611998877")),
            )

        assertEquals(listOf(ana), result)
    }

    /**
     * Matched with `sameNumber` rather than string equality, which is what
     * [org.carlospinan.bloqueador.app.calllog.numberRuleState] does: the rule may have been saved
     * from a call that arrived in national form while the contact card is international.
     */
    @Test
    fun `blocked matches a rule saved in another number format`() {
        val result =
            filterAgenda(
                book,
                AgendaFilter.BLOCKED,
                query = "",
                blockedNumbers = listOf(blocked("611 99 88 77")),
            )

        assertEquals(listOf(ana), result)
    }

    @Test
    fun `allowed keeps the contacts an allowlist rule matches`() {
        val result =
            filterAgenda(
                book,
                AgendaFilter.ALLOWED,
                query = "",
                allowlistedNumbers = listOf(allowed("600111222")),
            )

        assertEquals(listOf(bea), result)
    }

    /** The chip and the box are one narrowing, not two screens. */
    @Test
    fun `the chip and the query apply together`() {
        val result =
            filterAgenda(
                book,
                AgendaFilter.BLOCKED,
                query = "Ana",
                blockedNumbers = listOf(blocked("+34611998877"), blocked("600111222")),
            )

        assertEquals(listOf(ana), result)
    }

    @Test
    fun `a filter that matches nobody returns an empty list rather than everybody`() {
        assertEquals(emptyList(), filterAgenda(book, AgendaFilter.BLOCKED, query = ""))
    }
}
