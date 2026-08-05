package org.carlospinan.bloqueador.app.testing

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.carlospinan.bloqueador.app.db.AppDatabase

/**
 * A fresh in-memory [AppDatabase] with the real schema applied.
 *
 * Lives in `androidUnitTest` rather than `commonTest` because [JdbcSqliteDriver] is
 * JVM-only — repository tests that need a real SQLite engine can't be shared with iOS.
 *
 * Each call returns an independent database. To test that a value survives an app
 * restart, build a second repository over the *same* instance instead:
 *
 * ```
 * val db = createTestDatabase()
 * SqlSettingsRepository(db, Dispatchers.Unconfined).setBlockingEnabled(false)
 * assertFalse(SqlSettingsRepository(db, Dispatchers.Unconfined).blockingEnabled.first())  // re-hydrated from disk
 * ```
 */
internal fun createTestDatabase(): AppDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    AppDatabase.Schema.create(driver)
    return AppDatabase(driver)
}
