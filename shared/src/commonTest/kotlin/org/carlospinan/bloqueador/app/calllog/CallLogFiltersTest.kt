package org.carlospinan.bloqueador.app.calllog

import org.carlospinan.bloqueador.app.rules.CallDirection
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CallLogFiltersTest {
    private fun entry(
        id: Long,
        number: String,
        action: String = "ALLOWED",
        direction: CallDirection = CallDirection.INCOMING,
    ) = CallLogEntryData(
        id = id,
        number = number,
        timestamp = id,
        action = action,
        ruleType = null,
        ruleId = null,
        ruleDetail = null,
        direction = direction,
    )

    private val incoming = entry(1, "+34611998877")
    private val blocked = entry(2, "+34900111222", action = "BLOCKED")
    private val outgoing = entry(3, "+34600123456", direction = CallDirection.OUTGOING)
    private val log = listOf(incoming, blocked, outgoing)

    @Test
    fun `all keeps everything`() {
        assertEquals(log, filterCallLog(log, CallLogDirectionFilter.ALL, ""))
    }

    @Test
    fun `outgoing keeps only calls the user placed`() {
        assertEquals(listOf(outgoing), filterCallLog(log, CallLogDirectionFilter.OUTGOING, ""))
    }

    @Test
    fun `incoming keeps blocked calls too because they arrived`() {
        // A blocked call is an incoming call that was refused, not a third direction. Excluding
        // it here would make "Incoming" quietly mean "incoming and answered".
        assertEquals(listOf(incoming, blocked), filterCallLog(log, CallLogDirectionFilter.INCOMING, ""))
    }

    @Test
    fun `blocked is an outcome rather than a direction`() {
        assertEquals(listOf(blocked), filterCallLog(log, CallLogDirectionFilter.BLOCKED, ""))
    }

    @Test
    fun `search matches the digits of a number whatever its formatting`() {
        assertEquals(listOf(incoming), filterCallLog(log, CallLogDirectionFilter.ALL, "611 99"))
    }

    /** The row shows the contact's name, so that is what the user types to find it again. */
    @Test
    fun `search matches a contact name`() {
        val names = mapOf("34611998877" to "Ana Torres", "611998877" to "Ana Torres")

        val matches = filterCallLog(log, CallLogDirectionFilter.ALL, "ana", names)

        assertEquals(listOf(incoming), matches)
    }

    @Test
    fun `search and direction narrow together`() {
        assertTrue(filterCallLog(log, CallLogDirectionFilter.OUTGOING, "611998877").isEmpty())
        assertEquals(listOf(outgoing), filterCallLog(log, CallLogDirectionFilter.OUTGOING, "600123456"))
    }

    @Test
    fun `a blank query is not a filter`() {
        assertEquals(log, filterCallLog(log, CallLogDirectionFilter.ALL, "   "))
    }

    @Test
    fun `a query nothing matches returns nothing rather than everything`() {
        assertTrue(filterCallLog(log, CallLogDirectionFilter.ALL, "zzz").isEmpty())
    }
}
