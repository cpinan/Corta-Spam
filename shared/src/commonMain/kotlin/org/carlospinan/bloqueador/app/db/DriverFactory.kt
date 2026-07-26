package org.carlospinan.bloqueador.app.db

import app.cash.sqldelight.db.SqlDriver

interface DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): AppDatabase = AppDatabase(driverFactory.createDriver())
