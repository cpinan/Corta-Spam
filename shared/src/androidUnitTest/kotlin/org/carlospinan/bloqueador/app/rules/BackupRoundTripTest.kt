package org.carlospinan.bloqueador.app.rules

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.db.AppDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupRoundTripTest {
    private fun createDatabase(): AppDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return AppDatabase(driver)
    }

    @Test
    fun roundTrip_preservesAllRules() =
        runTest {
            val originalRepo = SqlRuleRepository(createDatabase(), Dispatchers.Unconfined)
            originalRepo.addBlockedNumber("+34600123456", "spam")
            originalRepo.addAllowlistedNumber("+34600123457", "friend")
            originalRepo.addPatternRule("+34900*", "Spanish spam")
            originalRepo.togglePatternRule(1, false)
            originalRepo.addCountryRule("1", "United States")
            originalRepo.addActionRule("repeat", 3, 5, null)
            originalRepo.addScheduleRule("night", 1320, 420)

            val json = originalRepo.exportAll()

            val importRepo = SqlRuleRepository(createDatabase(), Dispatchers.Unconfined)
            val result = importRepo.importAll(json)

            assertEquals(1, result.blockedNumbersImported)
            assertEquals(1, result.allowlistedNumbersImported)
            assertEquals(1, result.patternsImported)
            assertEquals(1, result.countriesImported)
            assertEquals(1, result.actionsImported)
            assertEquals(1, result.schedulesImported)
            assertEquals(6, result.total)

            val exportedBlocked = importRepo.blockedNumberEntries()
            assertEquals(1, exportedBlocked.size)
            assertEquals("+34600123456", exportedBlocked[0].number)
            assertEquals("spam", exportedBlocked[0].label)

            val exportedActions = importRepo.enabledActionRules()
            assertEquals(1, exportedActions.size)
            assertEquals(3, exportedActions[0].attempts)
            assertEquals(5, exportedActions[0].windowMinutes)
            assertEquals(null, exportedActions[0].patternId)
        }
}
