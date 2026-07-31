package org.carlospinan.bloqueador.app.db

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher

interface DriverFactory {
    fun createDriver(): SqlDriver
    val databaseDispatcher: CoroutineDispatcher
}

fun createDatabase(driverFactory: DriverFactory): AppDatabase = AppDatabase(driverFactory.createDriver())
