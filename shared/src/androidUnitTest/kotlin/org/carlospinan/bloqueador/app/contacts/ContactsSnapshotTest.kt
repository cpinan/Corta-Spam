package org.carlospinan.bloqueador.app.contacts

import org.carlospinan.bloqueador.app.rules.PhoneNumberParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The address-book scan feeds two different consumers with two different matching rules, and the
 * number set is the one that has been wrong: it is compared with [PhoneNumberParser.sameNumber],
 * which reads the leading `+` to decide whether a national form may bridge two numbers.
 */
class ContactsSnapshotTest {
    private fun snapshotOf(vararg rows: Pair<String, String?>) =
        buildContactsSnapshot(
            rows.asSequence().map { (number, name) -> ContactRow(number, name) },
        )

    /**
     * The regression. A contact saved internationally, called from a domestic line that delivers
     * the national form — the caller the resolver has to recognise as allowlisted before it ever
     * reaches a pattern rule.
     */
    @Test
    fun contactSavedInE164IsMatchedByANationalFormatCall() {
        val snapshot = snapshotOf("+34611998877" to "Ana Torres")

        assertTrue(
            snapshot.numbers.any { PhoneNumberParser.sameNumber(it, "611998877") },
            "a contact saved as +34611998877 must match an incoming 611998877; got ${snapshot.numbers}",
        )
    }

    @Test
    fun contactSavedInE164IsStillMatchedByAnE164Call() {
        val snapshot = snapshotOf("+34611998877" to "Ana Torres")

        assertTrue(snapshot.numbers.any { PhoneNumberParser.sameNumber(it, "+34611998877") })
    }

    @Test
    fun contactSavedNationallyIsMatchedByAnE164Call() {
        val snapshot = snapshotOf("611 99 88 77" to "Ana Torres")

        assertTrue(snapshot.numbers.any { PhoneNumberParser.sameNumber(it, "+34611998877") })
    }

    /**
     * The other half of [PhoneNumberParser.sameNumber]'s asymmetry: two international numbers
     * that share a national form are different people, and keeping the `+` is what preserves it.
     * Digit-normalising the set made this pair match.
     */
    @Test
    fun contactSavedInE164DoesNotMatchTheSameNationalNumberInAnotherCountry() {
        val snapshot = snapshotOf("+34611998877" to "Ana Torres")

        assertTrue(snapshot.numbers.none { PhoneNumberParser.sameNumber(it, "+51611998877") })
    }

    @Test
    fun namesAreKeyedByEveryComparisonForm() {
        val snapshot = snapshotOf("+34611998877" to "Ana Torres")

        assertEquals("Ana Torres", contactDisplayName("611998877", snapshot.names))
        assertEquals("Ana Torres", contactDisplayName("+34611998877", snapshot.names))
    }

    @Test
    fun blankAndUnusableRowsAreDropped() {
        val snapshot = snapshotOf("" to "Blank", "   " to "Spaces", "no digits" to "Letters")

        assertTrue(snapshot.numbers.isEmpty())
        assertTrue(snapshot.contacts.isEmpty())
    }

    @Test
    fun theSameNumberUnderTwoNamesStaysTwoContacts() {
        val snapshot = snapshotOf("+34911223344" to "Casa", "+34911223344" to "Papá")

        assertEquals(listOf("Casa", "Papá"), snapshot.contacts.map { it.name })
        assertEquals(1, snapshot.numbers.size)
    }

    @Test
    fun theSameNameAndNumberSyncedTwiceIsOneContact() {
        val snapshot = snapshotOf("+34911223344" to "Casa", "+34 911 22 33 44" to "Casa")

        assertEquals(1, snapshot.contacts.size)
    }

    @Test
    fun contactsAreSortedWithAccentsInTheirAlphabeticalPlace() {
        val snapshot =
            snapshotOf(
                "+34600000001" to "Zoe",
                "+34600000002" to "Ángela",
                "+34600000003" to "Bruno",
            )

        assertEquals(listOf("Ángela", "Bruno", "Zoe"), snapshot.contacts.map { it.name })
    }

    @Test
    fun theFirstCardToClaimAKeyKeepsTheName() {
        val snapshot = snapshotOf("+34611998877" to "Ana Torres", "611998877" to "Somebody else")

        assertEquals("Ana Torres", contactDisplayName("+34611998877", snapshot.names))
    }
}
