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

        database.appDatabaseQueries.insertBlockedNumber("+34600123456", "Test block")
        val blocked = database.appDatabaseQueries.selectAllBlockedNumbers().executeAsList()

        assertEquals(1, blocked.size)
        assertEquals("+34600123456", blocked.first().number)
        assertEquals("Test block", blocked.first().label)
    }

    @Test
    fun insertAndQueryAllowlistedNumber() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val database = AppDatabase(driver)

        database.appDatabaseQueries.insertAllowlistedNumber("+34600999999", "Mom")
        val allowlisted = database.appDatabaseQueries.selectAllAllowlistedNumbers().executeAsList()

        assertEquals(1, allowlisted.size)
        assertEquals("+34600999999", allowlisted.first().number)
    }

    @Test
    fun insertCallLogEntry() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val database = AppDatabase(driver)

        database.appDatabaseQueries.insertCallLogEntry(
            number = "+34900123456",
            timestamp = System.currentTimeMillis(),
            action = "BLOCKED",
            rule_type = "MANUAL",
            rule_id = null,
            rule_detail = "Manually blocked",
        )
        val entries = database.appDatabaseQueries.selectAllCallLogEntries().executeAsList()

        assertEquals(1, entries.size)
        assertEquals("BLOCKED", entries.first().action)
        assertEquals("MANUAL", entries.first().rule_type)
    }

    @Test
    fun duplicateBlockedNumberIsIgnored() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        val database = AppDatabase(driver)

        database.appDatabaseQueries.insertBlockedNumber("+34600123456", null)
        database.appDatabaseQueries.insertBlockedNumber("+34600123456", "Second try")
        val blocked = database.appDatabaseQueries.selectAllBlockedNumbers().executeAsList()

        assertEquals(1, blocked.size)
    }
}
