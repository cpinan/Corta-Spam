package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.testing.createTestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Backup restore against a real SQLite engine.
 *
 * `BackupRoundTripTest` covers the happy path (export mine, import it somewhere else, get the
 * same rules). This covers the cases a hand-written or partially-overlapping backup produces,
 * every one of which used to corrupt the rule set or lie about what it did.
 */
class BackupImportTest {
    private fun repository() = SqlRuleRepository(createTestDatabase(), Dispatchers.Unconfined)

    @Test
    fun importingADisabledRuleDisablesThatRuleAndNoOther() =
        runTest {
            val repo = repository()

            val result =
                repo.importAll(
                    backup(
                        patternRules =
                            """
                            {"pattern": "111*", "enabled": true,  "createdAt": 100},
                            {"pattern": "222*", "enabled": true,  "createdAt": 200},
                            {"pattern": "333*", "enabled": false, "createdAt": 300}
                            """.trimIndent(),
                    ),
                )

            assertEquals(3, result.patternsImported)
            // The regression: the importer used to write a default (enabled) row and then look
            // for "the one I just inserted" as the last element of a DESC-ordered query, which
            // is the oldest row -- so "111*" got switched off and "333*" stayed on.
            assertEquals(
                setOf("111*", "222*"),
                repo.enabledPatterns().map { it.pattern }.toSet(),
            )
            assertEquals(
                false,
                repo
                    .patternRules()
                    .first()
                    .single { it.pattern == "333*" }
                    .enabled,
            )
        }

    @Test
    fun importingADisabledCountryRuleDisablesThatCountryAndNoOther() =
        runTest {
            val repo = repository()

            repo.importAll(
                backup(
                    countryRules =
                        """
                        {"countryCode": "1",  "countryName": "United States", "enabled": true,  "createdAt": 100},
                        {"countryCode": "44", "countryName": "United Kingdom", "enabled": false, "createdAt": 200}
                        """.trimIndent(),
                ),
            )

            // selectAllCountryRules orders by country_name ASC, so the old lastOrNull() picked
            // whichever country sorted last alphabetically rather than the row just written.
            assertEquals(listOf("1"), repo.enabledCountryRules().map { it.countryCode })
        }

    @Test
    fun createdAtSurvivesTheRoundTripSoListOrderIsPreserved() =
        runTest {
            val repo = repository()

            repo.importAll(
                backup(
                    blockedNumbers =
                        """
                        {"number": "+34600000001", "createdAt": 1000},
                        {"number": "+34600000002", "createdAt": 2000}
                        """.trimIndent(),
                ),
            )

            val entries = repo.blockedNumberEntries()
            assertEquals(listOf(2000L, 1000L), entries.map { it.createdAt }, "newest first, from the backup's own timestamps")
        }

    @Test
    fun aMissingCreatedAtIsStampedWithNowRatherThanTheEpoch() =
        runTest {
            val repo = repository()

            repo.importAll(backup(blockedNumbers = """{"number": "+34600000001", "createdAt": 0}"""))

            assertTrue(repo.blockedNumberEntries().single().createdAt > 0)
        }

    @Test
    fun alreadyPresentNumbersAreCountedAsSkippedNotImported() =
        runTest {
            val repo = repository()
            repo.addBlockedNumber("+34600123456", "spam")

            val result =
                repo.importAll(
                    backup(
                        blockedNumbers =
                            """
                            {"number": "+34600123456", "createdAt": 100},
                            {"number": "+34600999999", "createdAt": 200}
                            """.trimIndent(),
                    ),
                )

            // INSERT OR IGNORE against UNIQUE(number): one landed, one didn't. The loop counter
            // used to report both, so re-importing your own backup claimed to add rules twice.
            assertEquals(1, result.blockedNumbersImported)
            assertEquals(1, result.skipped)
            assertEquals(2, repo.blockedNumberEntries().size)
        }

    @Test
    fun anActionRuleThatWouldMatchEveryCallIsRejected() =
        runTest {
            val repo = repository()

            val result =
                repo.importAll(
                    backup(
                        actionRules =
                            """
                            {"attempts": 0, "windowMinutes": 5, "createdAt": 100},
                            {"attempts": 3, "windowMinutes": 0, "createdAt": 200},
                            {"attempts": 3, "windowMinutes": 5, "createdAt": 300}
                            """.trimIndent(),
                    ),
                )

            // attempts=0 makes `count >= attempts` true for every caller; the UI can't produce
            // it, but a hand-edited backup can, and it would block the entire phone.
            assertEquals(1, result.actionsImported)
            assertEquals(2, result.skipped)
            assertEquals(listOf(3), repo.enabledActionRules().map { it.attempts })
        }

    @Test
    fun aQuietHoursWindowOutsideTheDayIsRejected() =
        runTest {
            val repo = repository()

            val result =
                repo.importAll(
                    backup(
                        scheduleRules =
                            """
                            {"startMinute": -10, "endMinute": 420, "createdAt": 100},
                            {"startMinute": 1320, "endMinute": 5000, "createdAt": 200},
                            {"startMinute": 1320, "endMinute": 420, "createdAt": 300}
                            """.trimIndent(),
                    ),
                )

            assertEquals(1, result.schedulesImported)
            assertEquals(2, result.skipped)
            assertEquals(listOf(1320), repo.enabledScheduleRules().map { it.startMinute })
        }

    @Test
    fun aPatternThatWouldMatchEveryNumberIsRejected() =
        runTest {
            val repo = repository()

            val result =
                repo.importAll(
                    backup(
                        patternRules =
                            """
                            {"pattern": "*", "enabled": true, "createdAt": 100},
                            {"pattern": "*abc*", "enabled": true, "createdAt": 200},
                            {"pattern": "+34900*", "enabled": true, "createdAt": 300}
                            """.trimIndent(),
                    ),
                )

            assertEquals(1, result.patternsImported)
            assertEquals(2, result.skipped)
            assertEquals(listOf("+34900*"), repo.enabledPatterns().map { it.pattern })
        }

    @Test
    fun anActionRulesPatternScopeIsRelinkedToTheNewlyAssignedPatternId() =
        runTest {
            val source = repository()
            source.addPatternRule("+34900*", "Spanish spam")
            val sourcePatternId =
                source
                    .patternRules()
                    .first()
                    .single()
                    .id
            source.addActionRule("scoped", attempts = 3, windowMinutes = 5, patternId = sourcePatternId)

            // Give the destination a pattern of its own first, so ids can't line up by accident.
            val destination = repository()
            destination.addPatternRule("+1555*", "unrelated")

            destination.importAll(source.exportAll())

            val importedPattern = destination.patternRules().first().single { it.pattern == "+34900*" }
            val importedAction = destination.enabledActionRules().single()
            assertEquals(
                importedPattern.id,
                importedAction.patternId,
                "the scope must follow the pattern, not the raw id from the exporting device",
            )
        }

    @Test
    fun anUnresolvablePatternScopeIsDroppedRatherThanLeftDangling() =
        runTest {
            val repo = repository()
            repo.addPatternRule("+1555*", "already here")

            // patternId 1 exists in this database but was never part of the backup, so the
            // reference means nothing -- honouring it would silently rescope the rule.
            repo.importAll(backup(actionRules = """{"attempts": 3, "windowMinutes": 5, "patternId": 1, "createdAt": 100}"""))

            assertNull(repo.enabledActionRules().single().patternId)
        }

    @Test
    fun anInvalidEntryDoesNotLeaveThePreviousOnesHalfWritten() =
        runTest {
            val repo = repository()

            val result =
                repo.importAll(
                    backup(
                        blockedNumbers = """{"number": "+34600000001", "createdAt": 100}""",
                        scheduleRules = """{"startMinute": 9999, "endMinute": 420, "createdAt": 200}""",
                    ),
                )

            // A rejected entry is skipped, not fatal: everything valid in the file still lands.
            assertEquals(1, result.blockedNumbersImported)
            assertEquals(0, result.schedulesImported)
            assertEquals(1, result.skipped)
            assertEquals(1, repo.blockedNumberEntries().size)
        }

    @Test
    fun malformedJsonImportsNothingAtAll() =
        runTest {
            val repo = repository()
            repo.addBlockedNumber("+34600123456", null)

            val threw =
                try {
                    repo.importAll("{ not json at all")
                    false
                } catch (_: Exception) {
                    true
                }

            assertTrue(threw)
            assertEquals(1, repo.blockedNumberEntries().size, "the pre-existing rule set is untouched")
        }

    private fun backup(
        blockedNumbers: String = "",
        allowlistedNumbers: String = "",
        patternRules: String = "",
        countryRules: String = "",
        actionRules: String = "",
        scheduleRules: String = "",
    ): String =
        """
        {
          "version": 1,
          "exportedAt": 0,
          "blockedNumbers": [$blockedNumbers],
          "allowlistedNumbers": [$allowlistedNumbers],
          "patternRules": [$patternRules],
          "countryRules": [$countryRules],
          "actionRules": [$actionRules],
          "scheduleRules": [$scheduleRules]
        }
        """.trimIndent()
}
