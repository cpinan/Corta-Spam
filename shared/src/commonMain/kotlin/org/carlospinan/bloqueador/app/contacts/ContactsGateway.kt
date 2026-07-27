package org.carlospinan.bloqueador.app.contacts

/**
 * Platform abstraction for reading local device contacts.
 * Used to auto-allow contacts in the resolver (SPEC §2 gap #1).
 */
interface ContactsGateway {
    /** Returns phone numbers from the user's local contacts, normalized. */
    suspend fun contactNumbers(): Set<String>

    /** Whether the platform has the required permission granted. */
    fun hasPermission(): Boolean
}
