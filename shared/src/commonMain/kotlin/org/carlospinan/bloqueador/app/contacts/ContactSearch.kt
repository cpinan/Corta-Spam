package org.carlospinan.bloqueador.app.contacts

import org.carlospinan.bloqueador.app.rules.PhoneNumberParser

/**
 * Filters the address book for the keypad's one input field.
 *
 * That field is both a search box and the number being dialled, so a query has to be matched two
 * ways at once: "ana" against names, "611" against numbers. Deciding which the user meant by
 * looking at the query is the wrong call — a contact saved as "O2" is a name with a digit in it,
 * and a name typed on a phone keypad is still a name. Both matchers always run and the results
 * are merged.
 *
 * Numbers are compared as digits only, on both sides, so the stored formatting is irrelevant:
 * "611" finds "+34 611 99 88 77". This is a substring match rather than [PhoneNumberParser]'s
 * [PhoneNumberParser.sameNumber] on purpose — that answers "are these the same number", and a
 * half-typed prefix is by definition not one yet.
 */
object ContactSearch {
    fun match(
        contacts: List<Contact>,
        query: String,
    ): List<Contact> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val queryDigits = PhoneNumberParser.normalizeForComparison(trimmed)

        return contacts
            .mapNotNull { contact ->
                val rank = rank(contact, trimmed, queryDigits)
                if (rank == NO_MATCH) null else contact to rank
            }
            // sortedBy is stable, so contacts of equal rank keep the order the gateway sorted
            // them into (by name) rather than being reshuffled on every keystroke.
            .sortedBy { it.second }
            .map { it.first }
    }

    /**
     * Lower is better. A prefix match is ranked above a match buried in the middle because
     * someone typing "ana" wants Ana before Juana, and someone typing "611" wants the number
     * that starts with it before the one that merely contains it.
     */
    private fun rank(
        contact: Contact,
        query: String,
        queryDigits: String,
    ): Int {
        val name = contact.name
        if (name.startsWith(query, ignoreCase = true)) return NAME_PREFIX
        val numberDigits =
            if (queryDigits.isEmpty()) "" else PhoneNumberParser.normalizeForComparison(contact.number)
        if (queryDigits.isNotEmpty() && numberDigits.startsWith(queryDigits)) return NUMBER_PREFIX
        // Word-start rather than bare contains, so "ana" matches "Maria Ana" but not "Susana".
        // A surname is what the user types second; a syllable inside a word is a coincidence.
        if (name.split(' ', '-').any { it.startsWith(query, ignoreCase = true) }) return NAME_WORD_PREFIX
        // A stored international number is found by its national part too: the address book says
        // "+34611998877" and the user types the "611998877" they know by heart.
        if (queryDigits.isNotEmpty() && numberDigits.contains(queryDigits)) return NUMBER_CONTAINS
        return NO_MATCH
    }

    private const val NAME_PREFIX = 0
    private const val NUMBER_PREFIX = 1
    private const val NAME_WORD_PREFIX = 2
    private const val NUMBER_CONTAINS = 3
    private const val NO_MATCH = Int.MAX_VALUE
}
