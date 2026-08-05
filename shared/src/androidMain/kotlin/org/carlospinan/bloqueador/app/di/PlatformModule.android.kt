package org.carlospinan.bloqueador.app.di

import org.carlospinan.bloqueador.app.autoresponder.AutoResponderDefaults
import org.carlospinan.bloqueador.app.contacts.AndroidContactsGateway
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.db.AndroidDriverFactory
import org.carlospinan.bloqueador.app.db.DriverFactory
import org.carlospinan.bloqueador.app.onboarding.AndroidDefaultDialerGateway
import org.carlospinan.bloqueador.app.onboarding.DefaultDialerGateway
import org.carlospinan.bloqueador.app.onboarding.DialerOnboardingViewModel
import org.carlospinan.bloqueador.app.shared.R
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<DriverFactory> { AndroidDriverFactory(androidContext()) }
        single<DefaultDialerGateway> { AndroidDefaultDialerGateway(androidContext()) }
        single<ContactsGateway> { AndroidContactsGateway(androidContext()) }
        factory { DialerOnboardingViewModel(get(), get()) }
        // Read through the Android resource system so the untouched greeting is in the user's
        // language rather than the English constant baked into commonMain.
        single {
            AutoResponderDefaults(
                script = androidContext().getString(R.string.auto_responder_default_script),
            )
        }
    }
