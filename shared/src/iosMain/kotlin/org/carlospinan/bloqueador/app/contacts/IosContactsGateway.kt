package org.carlospinan.bloqueador.app.contacts

class IosContactsGateway : ContactsGateway {
    override suspend fun contactNumbers(): Set<String> = emptySet()

    override suspend fun contactNames(): Map<String, String> = emptyMap()

    override suspend fun contacts(): List<Contact> = emptyList()

    override fun hasPermission(): Boolean = false
}
