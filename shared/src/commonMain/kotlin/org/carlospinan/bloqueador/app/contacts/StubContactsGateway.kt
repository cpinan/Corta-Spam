package org.carlospinan.bloqueador.app.contacts

/**
 * No-op implementation for platforms where contacts access is not yet wired.
 * Returns empty set — contacts won't bypass blocks until the real impl is in.
 */
class StubContactsGateway : ContactsGateway {
    override suspend fun contactNumbers(): Set<String> = emptySet()

    override fun hasPermission(): Boolean = false
}
