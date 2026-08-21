package org.carlospinan.bloqueador.app.agenda

import org.carlospinan.bloqueador.app.calllog.numberRuleState
import org.carlospinan.bloqueador.app.contacts.Contact
import org.carlospinan.bloqueador.app.contacts.ContactSearch
import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry

/**
 * What the address book is narrowed to on the Agenda tab.
 *
 * [STARRED] is the platform's own favourites, not a list this app keeps. [BLOCKED] and [ALLOWED]
 * are this app's rules asked from the other end: the block list answers "which numbers did I
 * block", and it answers it in numbers, which is no help at all when the user remembers a person.
 * A contact who is blocked is the single most interesting row in an address book belonging to a
 * call blocker, and until this filter existed there was no screen that could show it.
 */
enum class AgendaFilter {
    ALL,
    STARRED,
    BLOCKED,
    ALLOWED,
}

/**
 * Applies the chip and the search box, in that order.
 *
 * Pure, and outside the ViewModel, because both inputs change as fast as the user types and
 * neither touches the database — the contacts are already in memory and the rule lists arrive as
 * flows.
 *
 * The rule filters are evaluated *after* the query has narrowed the list. Each one is a
 * `sameNumber` scan of the whole rule set per contact, and running that across a 5000-entry
 * address book on every keystroke is work the query has usually already made unnecessary.
 */
fun filterAgenda(
    contacts: List<Contact>,
    filter: AgendaFilter,
    query: String,
    blockedNumbers: List<BlockedNumberEntry> = emptyList(),
    allowlistedNumbers: List<AllowlistedNumberEntry> = emptyList(),
): List<Contact> {
    // A blank query is the whole book, not nothing: [ContactSearch.match] answers "what matches
    // what is being typed", and its empty answer for an empty query is right for the keypad and
    // wrong for a screen whose resting state is the full list.
    val byQuery = if (query.isBlank()) contacts else ContactSearch.match(contacts, query)

    return when (filter) {
        AgendaFilter.ALL -> byQuery
        AgendaFilter.STARRED -> byQuery.filter { it.starred }
        AgendaFilter.BLOCKED ->
            byQuery.filter {
                numberRuleState(it.number, blockedNumbers, allowlistedNumbers).isBlocked
            }
        AgendaFilter.ALLOWED ->
            byQuery.filter {
                numberRuleState(it.number, blockedNumbers, allowlistedNumbers).isAllowlisted
            }
    }
}
