package org.carlospinan.bloqueador.app.di

import org.carlospinan.bloqueador.app.blocklist.BlockListViewModel
import org.carlospinan.bloqueador.app.calllog.CallLogViewModel
import org.carlospinan.bloqueador.app.contacts.ContactsGateway
import org.carlospinan.bloqueador.app.contacts.StubContactsGateway
import org.carlospinan.bloqueador.app.db.createDatabase
import org.carlospinan.bloqueador.app.home.HomeViewModel
import org.carlospinan.bloqueador.app.rules.CallLogRepository
import org.carlospinan.bloqueador.app.rules.RuleRepository
import org.carlospinan.bloqueador.app.rules.SqlCallLogRepository
import org.carlospinan.bloqueador.app.rules.SqlRuleRepository
import org.koin.dsl.module

/** Shared module: repositories, ViewModels, and resolver. Bound after platform module provides DriverFactory + AppDatabase. */
val sharedModule =
    module {
        single { createDatabase(get()) }

        single<RuleRepository> { SqlRuleRepository(get()) }
        single<CallLogRepository> { SqlCallLogRepository(get()) }

        single<ContactsGateway> { StubContactsGateway() }

        factory { HomeViewModel(get()) }
        factory { BlockListViewModel(get()) }
        factory { CallLogViewModel(get()) }
    }
