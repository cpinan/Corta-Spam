package org.carlospinan.bloqueador.app.di

import org.carlospinan.bloqueador.app.agenda.AgendaViewModel
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderRepository
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderViewModel
import org.carlospinan.bloqueador.app.autoresponder.SqlAutoResponderRepository
import org.carlospinan.bloqueador.app.backup.BackupViewModel
import org.carlospinan.bloqueador.app.blocklist.BlockListViewModel
import org.carlospinan.bloqueador.app.calllog.CallLogViewModel
import org.carlospinan.bloqueador.app.db.DriverFactory
import org.carlospinan.bloqueador.app.db.createDatabase
import org.carlospinan.bloqueador.app.home.HomeViewModel
import org.carlospinan.bloqueador.app.keypad.KeypadViewModel
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.RuleRepository
import org.carlospinan.bloqueador.app.rules.SqlCallLogRepository
import org.carlospinan.bloqueador.app.rules.SqlRuleRepository
import org.carlospinan.bloqueador.app.rules.domain.EvaluateIncomingCallUseCase
import org.carlospinan.bloqueador.app.settings.SettingsRepository
import org.carlospinan.bloqueador.app.settings.SettingsViewModel
import org.carlospinan.bloqueador.app.settings.SqlSettingsRepository
import org.carlospinan.bloqueador.app.spam.BundledSpamProvider
import org.carlospinan.bloqueador.app.spam.SpamProviderClient
import org.carlospinan.bloqueador.app.spam.SpamProviderRepository
import org.carlospinan.bloqueador.app.spam.SqlSpamProviderRepository
import org.carlospinan.bloqueador.app.stats.StatsViewModel
import org.koin.dsl.module

/** Shared module: repositories, ViewModels, and resolver. Bound after platform module provides DriverFactory + AppDatabase. */
val sharedModule =
    module {
        single { createDatabase(get()) }

        // SQLite work is blocking I/O, so it belongs on the platform's I/O pool -- not on
        // Dispatchers.Default, whose thread count is the core count and which Compose also uses
        // for recomposition work. DriverFactory has always declared the right dispatcher for its
        // platform; nothing read it until now.
        single { get<DriverFactory>().databaseDispatcher }

        single<RuleRepository> { SqlRuleRepository(get(), get()) }
        single<CallLogRepository> { SqlCallLogRepository(get(), get()) }
        single<SettingsRepository> { SqlSettingsRepository(get(), get()) }
        single<SpamProviderRepository> { SqlSpamProviderRepository(get(), get()) }
        single<SpamProviderClient> { BundledSpamProvider() }
        single<AutoResponderRepository> { SqlAutoResponderRepository(get(), get(), get()) }
        single { EvaluateIncomingCallUseCase(get(), get(), get(), get(), get()) }

        factory { HomeViewModel(get(), get()) }
        factory { BlockListViewModel(get(), get()) }
        factory { CallLogViewModel(get(), get()) }
        factory { SettingsViewModel(get(), get()) }
        factory { AutoResponderViewModel(get()) }
        factory { StatsViewModel(get()) }
        factory { KeypadViewModel(get(), get(), get()) }
        factory { AgendaViewModel(get(), get()) }
        factory { BackupViewModel(get()) }
    }
