package org.carlospinan.bloqueador.app.contacts

import org.carlospinan.bloqueador.app.rules.PhoneNumberParser
import java.text.Collator

/** One row of `ContactsContract.CommonDataKinds.Phone` — the number as saved, and its card's name. */
internal data class ContactRow(
    val number: String?,
    val displayName: String?,
    val starred: Boolean = false,
)

/**
 * What one address-book scan produces: the numbers the rule engine matches against, the
 * name lookup every screen renders through, and the ordered list the contact picker shows.
 */
internal data class ContactsSnapshot(
    val numbers: Set<String>,
    val names: Map<String, String>,
    val contacts: List<Contact>,
)

/**
 * Turns raw contact rows into a [ContactsSnapshot]. Pure, so the mapping is testable without a
 * ContentResolver — the number-form handling below is the part that has actually been wrong.
 *
 * **[ContactsSnapshot.numbers] holds each number exactly as saved, not its digits.**
 * `RulePrecedenceResolver` compares that set with [PhoneNumberParser.sameNumber], whose whole
 * asymmetry rule reads the leading `+` to decide whether the *national* form may bridge two
 * numbers. Stripping punctuation here threw that away, so a contact saved `+34611998877` became
 * `34611998877`, which states no country — and a call arriving in national form (`611998877`,
 * which is how a domestic call is often delivered) no longer matched it. The contact was then
 * not allowlisted, and any pattern, country, quiet-hours or default-block rule blocked a caller
 * who was in the address book all along. The call log even showed their name, because
 * [ContactsSnapshot.names] was keyed correctly the whole time.
 */
internal fun buildContactsSnapshot(rows: Sequence<ContactRow>): ContactsSnapshot {
    val numbers = mutableSetOf<String>()
    val names = mutableMapOf<String, String>()
    val contacts = mutableListOf<Contact>()
    // Where each deduplicated contact ended up, so a later row for the same person can still
    // raise its star -- see below.
    val positions = mutableMapOf<Pair<String, String>, Int>()
    // One row per phone entry, and the same number arrives twice whenever a card is synced from
    // two accounts. Deduplicated on (name, digits) rather than on the number alone: a household
    // landline saved under two people is two search results, and dropping one of them hides a
    // contact the user went looking for.
    val seen = mutableSetOf<Pair<String, String>>()

    for (row in rows) {
        val raw = row.number
        if (raw.isNullOrBlank()) continue
        // Every form this contact may be recognised by, not just its digits: a card saved
        // "611 99 88 77" has to be found when "+34611998877" calls.
        val keys = PhoneNumberParser.comparisonKeys(raw)
        if (keys.isEmpty()) continue
        numbers.add(raw.trim())
        val name = row.displayName
        if (!name.isNullOrBlank()) {
            // putIfAbsent semantics: the first contact to claim a key keeps it, so a later card
            // sharing a national number cannot rename an exact match.
            keys.forEach { key -> names.getOrPut(key) { name } }
            val key = name to PhoneNumberParser.normalizeForComparison(raw)
            if (seen.add(key)) {
                positions[key] = contacts.size
                contacts.add(Contact(name = name, number = raw.trim(), starred = row.starred))
            } else if (row.starred) {
                // The same card synced from two accounts arrives as two rows, and only one of
                // them may carry the star -- Google's copy is starred, the SIM's copy is not.
                // Taking the first row's flag would drop a favourite depending on which account
                // the provider happened to return first.
                val index = positions.getValue(key)
                contacts[index] = contacts[index].copy(starred = true)
            }
        }
    }

    // Sorted here rather than by the cursor: ordering by DISPLAY_NAME in SQLite is bytewise,
    // which files every accented name after Z -- and "Ángela" belongs with the A's in all four
    // locales this app ships in. Collator is the locale-aware comparison.
    val byName = compareBy<Contact, String>(Collator.getInstance()) { it.name }
    return ContactsSnapshot(numbers, names, contacts.sortedWith(byName))
}
