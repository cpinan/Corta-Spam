package org.carlospinan.bloqueador.app.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseDriverTest {
    @Test
    fun schemaCreatesAndRoundTripsOnInMemoryDriver() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val database = AppDatabase(driver)

        database.appDatabaseQueries.insertVersion(1, 1)
        val rows = database.appDatabaseQueries.selectVersion().executeAsList()

        assertEquals(1, rows.size)
        assertEquals(1L, rows.first().version)
    }
}
