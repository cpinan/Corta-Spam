package org.carlospinan.bloqueador.app.keypad

import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.contactDisplayName
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.rules.PhoneNumberParser

/**
 * The people this phone dealt with most recently, for the strip above the keypad's number field.
 *
 * The strip shows the starred contacts when there are any. A phone with none — which is most of
 * them, since starring is a thing few people ever do — got an empty band instead, which is the
 * defect this exists to close: the keypad cannot fill a 1000 dp screen with twelve keys, so the
 * space is either used or blank, and a used phone always has a call log.
 *
 * Deduplicated by [PhoneNumberParser.comparisonKeys] rather than by the string or by its digits,
 * because the same person appears in the log as `+51987654321` and `987654321` depending on how
 * the call arrived, and a strip of four that spends two slots on one caller shows half as many
 * people as it looks like it does. Digits alone are not enough for that: `51987654321` and
 * `987654321` are different strings, and the keys are exactly the set of forms one number may be
 * recognised by.
 *
 * A number with no contact behind it keeps the number as its label. That is deliberate: the row it
 * came from is the user's own call log, and "who rang me twenty minutes ago" is a question a bare
 * number answers perfectly well.
 */
fun recentContacts(
    entries: List<CallLogEntryData>,
    contactNames: Map<String, String> = emptyMap(),
    limit: Int = RECENT_LIMIT,
): List<Contact> {
    val seen = mutableSetOf<String>()
    return entries
        .asSequence()
        // Sorted here rather than trusted from the query: this reads a repository flow whose
        // ordering is its own business, and a strip labelled "recent" that is not in time order is
        // worse than no strip.
        .sortedByDescending { it.timestamp }
        .filter { it.number.isNotBlank() }
        .filter { entry ->
            val keys = PhoneNumberParser.comparisonKeys(entry.number)
            if (keys.isEmpty() || keys.any { it in seen }) false else seen.addAll(keys)
        }.take(limit)
        .map { entry ->
            Contact(
                name = contactDisplayName(entry.number, contactNames),
                number = entry.number,
            )
        }.toList()
}

/**
 * How many fit across a phone without the strip scrolling on the common case. Four 72 dp columns
 * with their gaps come to roughly 350 dp, which is inside a 360 dp phone's width.
 */
private const val RECENT_LIMIT = 4
