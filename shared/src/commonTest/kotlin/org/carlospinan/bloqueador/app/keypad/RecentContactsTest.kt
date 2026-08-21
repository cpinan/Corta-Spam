package org.carlospinan.bloqueador.app.keypad

import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentContactsTest {
    private fun entry(
        number: String,
        timestamp: Long,
        id: Long = timestamp,
    ) = CallLogEntryData(
        id = id,
        number = number,
        timestamp = timestamp,
        action = "ALLOWED",
        ruleType = null,
        ruleId = null,
        ruleDetail = null,
    )

    @Test
    fun `the most recent callers come first`() {
        val recent =
            recentContacts(
                listOf(entry("600111222", 100), entry("600333444", 300), entry("600555666", 200)),
            )

        assertEquals(listOf("600333444", "600555666", "600111222"), recent.map { it.number })
    }

    /**
     * The same person reaches the log as `+51987654321` and `987654321` depending on how the call
     * arrived. A strip of four that spends two slots on one caller shows half as many people as it
     * looks like it does.
     */
    @Test
    fun `one caller takes one slot however their number was written`() {
        val recent =
            recentContacts(
                listOf(entry("+51987654321", 300), entry("987654321", 200), entry("600111222", 100)),
            )

        assertEquals(2, recent.size)
        assertEquals("+51987654321", recent.first().number)
    }

    @Test
    fun `the strip is capped`() {
        val recent = recentContacts(List(10) { entry("60011122$it", it.toLong()) })

        assertEquals(4, recent.size)
    }

    /** A withheld caller has no number to dial and no name to show. */
    @Test
    fun `blank numbers are left out`() {
        val recent = recentContacts(listOf(entry("", 300), entry("600111222", 200)))

        assertEquals(listOf("600111222"), recent.map { it.number })
    }

    @Test
    fun `a caller in the address book is shown by name`() {
        val recent =
            recentContacts(
                listOf(entry("+34611998877", 300)),
                contactNames = mapOf("34611998877" to "Ana Torres"),
            )

        assertEquals("Ana Torres", recent.single().name)
    }

    /** And one who is not keeps their number, which answers "who rang me" perfectly well. */
    @Test
    fun `a caller with no contact keeps the number as the label`() {
        val recent = recentContacts(listOf(entry("600111222", 300)))

        assertEquals("600111222", recent.single().name)
    }

    @Test
    fun `an empty log produces an empty strip`() {
        assertTrue(recentContacts(emptyList()).isEmpty())
    }
}
