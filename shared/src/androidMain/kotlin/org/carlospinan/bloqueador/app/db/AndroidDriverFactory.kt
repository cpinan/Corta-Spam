package org.carlospinan.bloqueador.app.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

class AndroidDriverFactory(
    private val context: Context,
) : DriverFactory {
    override fun createDriver(): SqlDriver = AndroidSqliteDriver(AppDatabase.Schema, context, "bloquellamadas.db")
}
