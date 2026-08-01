package org.carlospinan.bloqueador.app.di

import org.carlospinan.bloqueador.app.contacts.AndroidContactsGateway
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.db.AndroidDriverFactory
import org.carlospinan.bloqueador.app.db.DriverFactory
import org.carlospinan.bloqueador.app.onboarding.AndroidDefaultDialerGateway
import org.carlospinan.bloqueador.app.onboarding.DefaultDialerGateway
import org.carlospinan.bloqueador.app.onboarding.DialerOnboardingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<DriverFactory> { AndroidDriverFactory(androidContext()) }
        single<DefaultDialerGateway> { AndroidDefaultDialerGateway(androidContext()) }
        single<ContactsGateway> { AndroidContactsGateway(androidContext()) }
        factory { DialerOnboardingViewModel(get(), get()) }
    }
