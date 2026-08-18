package org.carlospinan.bloqueador.app.di

import org.carlospinan.bloqueador.app.autoresponder.AutoResponderDefaults
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.contacts.IosContactsGateway
import org.carlospinan.bloqueador.app.db.DriverFactory
import org.carlospinan.bloqueador.app.db.IosDriverFactory
import org.carlospinan.bloqueador.app.settings.AppVersion
import org.carlospinan.bloqueador.app.settings.readAppVersion
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<DriverFactory> { IosDriverFactory() }
        single<ContactsGateway> { IosContactsGateway() }
        single<AppVersion> { readAppVersion() }
        // English fallback while the iOS target is parked; Android supplies a localized one.
        single { AutoResponderDefaults() }
    }
