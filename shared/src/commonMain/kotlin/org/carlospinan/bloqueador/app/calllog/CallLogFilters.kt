package org.carlospinan.bloqueador.app.calllog

import org.carlospinan.bloqueador.app.contacts.contactDisplayName
import org.carlospinan.bloqueador.app.rules.CallDirection
import org.carlospinan.bloqueador.app.rules.CallLogEntryData
import org.carlospinan.bloqueador.app.rules.PhoneNumberParser

/**
 * What the call log is narrowed to, along the one axis a phone log is usually read by.
 *
 * Direction and outcome share an enum rather than being two independent filters because they are
 * one question in practice -- "who did I miss", "what got blocked", "who did I ring" -- and two
 * chip rows that can contradict each other ("outgoing" + "blocked" matches nothing, ever) is a
 * UI that lets the user build an empty screen and wonder what broke.
 */
enum class CallLogDirectionFilter {
    ALL,
    INCOMING,
    OUTGOING,
    BLOCKED,
}

/**
 * Applies the in-screen filters: direction/outcome first, then the free-text query.
 *
 * Pure, and separate from the ViewModel's date filtering, because these two run at different
 * moments: the date window is a navigation destination (Home's cards link straight to "today"),
 * while these change as fast as the user types. Keeping them here means the typing path never
 * touches the database.
 */
fun filterCallLog(
    entries: List<CallLogEntryData>,
    filter: CallLogDirectionFilter,
    query: String,
    contactNames: Map<String, String> = emptyMap(),
): List<CallLogEntryData> {
    val byDirection =
        when (filter) {
            CallLogDirectionFilter.ALL -> entries
            // Blocked is an outcome, not a direction, and it only exists for incoming calls --
            // so it is not "incoming AND blocked", it is simply every blocked row.
            CallLogDirectionFilter.BLOCKED -> entries.filter { it.action == "BLOCKED" }
            CallLogDirectionFilter.OUTGOING -> entries.filter { it.direction == CallDirection.OUTGOING }
            CallLogDirectionFilter.INCOMING -> entries.filter { it.direction == CallDirection.INCOMING }
        }

    val trimmed = query.trim()
    if (trimmed.isEmpty()) return byDirection

    val queryDigits = PhoneNumberParser.normalizeForComparison(trimmed)
    return byDirection.filter { entry ->
        // The name is what the row shows, so it is what the user is searching against -- a log
        // reading "Ana Torres" that only matched on digits would look like a broken search box.
        val name = contactDisplayName(entry.number, contactNames)
        if (name.contains(trimmed, ignoreCase = true)) return@filter true
        queryDigits.isNotEmpty() &&
            PhoneNumberParser.normalizeForComparison(entry.number).contains(queryDigits)
    }
}
