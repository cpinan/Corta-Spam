package org.carlospinan.bloqueador.app.contacts

/**
 * Platform abstraction for reading local device contacts.
 * Used to auto-allow contacts in the resolver (SPEC §2 gap #1).
 */
interface ContactsGateway {
    /** Returns phone numbers from the user's local contacts, normalized. */
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
