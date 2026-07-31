package org.carlospinan.bloqueador.app.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class IosDriverFactory : DriverFactory {
    override fun createDriver(): SqlDriver = NativeSqliteDriver(AppDatabase.Schema, "bloquellamadas.db")

    override val databaseDispatcher: CoroutineDispatcher = Dispatchers.Default
}
