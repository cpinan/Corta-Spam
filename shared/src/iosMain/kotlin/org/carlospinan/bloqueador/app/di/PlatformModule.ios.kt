package org.carlospinan.bloqueador.app.di

import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.contacts.IosContactsGateway
import org.carlospinan.bloqueador.app.db.DriverFactory
import org.carlospinan.bloqueador.app.db.IosDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<DriverFactory> { IosDriverFactory() }
        single<ContactsGateway> { IosContactsGateway() }
    }
