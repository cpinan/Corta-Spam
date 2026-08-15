package org.carlospinan.bloqueador.app.contacts

/**
 * Platform abstraction for reading local device contacts.
 * Used to auto-allow contacts in the resolver (SPEC §2 gap #1).
 */
interface ContactsGateway {
    /**
     * Phone numbers from the user's local contacts, **exactly as saved on the card**.
     *
     * Not normalized, and not digits. `RulePrecedenceResolver` compares this set with
     * [org.carlospinan.bloqueador.app.rules.PhoneNumberParser.sameNumber], which reads the
     * leading `+` to decide whether the national form may bridge two numbers — stripping it here
     * left a contact saved internationally unmatched by the same subscriber calling in national
     * form, so patterns and quiet hours blocked people who were in the address book.
     */
    suspend fun contactNumbers(): Set<String>

    /** Normalized number -> contact display name, for showing names instead of raw numbers in UI. */
    suspend fun contactNames(): Map<String, String>

    /**
     * Every contact that has a number, name and number as saved, sorted by name.
     *
     * Separate from [contactNames] because that map is keyed for lookup and has lost both the
     * ordering and the number's original formatting by the time it is built -- neither of which a
     * user reading a list of search results can do without.
     */
    suspend fun contacts(): List<Contact>

    /** Whether the platform has the required permission granted. */
    fun hasPermission(): Boolean
}
