package org.carlospinan.bloqueador.app.contacts

class IosContactsGateway : ContactsGateway {
    override suspend fun contactNumbers(): Set<String> = emptySet()

    override fun hasPermission(): Boolean = false
}
