package org.carlospinan.bloqueador.app.di

import org.carlospinan.bloqueador.app.autoresponder.AutoResponderRepository
import org.carlospinan.bloqueador.app.autoresponder.AutoResponderViewModel
import org.carlospinan.bloqueador.app.autoresponder.SqlAutoResponderRepository
import org.carlospinan.bloqueador.app.backup.BackupViewModel
import org.carlospinan.bloqueador.app.blocklist.BlockListViewModel
import org.carlospinan.bloqueador.app.calllog.CallLogViewModel
import org.carlospinan.bloqueador.app.db.createDatabase
import org.carlospinan.bloqueador.app.home.HomeViewModel
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

        single<RuleRepository> { SqlRuleRepository(get()) }
        single<CallLogRepository> { SqlCallLogRepository(get()) }
        single<SettingsRepository> { SqlSettingsRepository(get()) }
        single<SpamProviderRepository> { SqlSpamProviderRepository(get()) }
        single<SpamProviderClient> { BundledSpamProvider() }
        single<AutoResponderRepository> { SqlAutoResponderRepository(get()) }
        single { EvaluateIncomingCallUseCase(get(), get(), get(), get(), get()) }

        factory { HomeViewModel(get(), get()) }
        factory { BlockListViewModel(get()) }
        factory { CallLogViewModel(get()) }
        factory { SettingsViewModel(get(), get()) }
        factory { AutoResponderViewModel(get()) }
        factory { StatsViewModel(get()) }
        factory { BackupViewModel(get()) }
    }
