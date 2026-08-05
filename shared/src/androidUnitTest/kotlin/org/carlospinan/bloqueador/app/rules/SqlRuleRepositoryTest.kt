package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.testing.createTestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The real [SqlRuleRepository] against a real SQLite engine.
 *
 * `BackupRoundTripTest` already covers `exportAll`/`importAll`; this covers the rest of the
 * interface — the CRUD and, more importantly, the two things a fake can't check: that each
 * `enabled*` snapshot really filters on the `enabled` column (the rule engine reads those, so a
 * disabled rule leaking through would block calls the user switched off), and that the reactive
 * flows and the suspend snapshots agree about the same rows.
 */
class SqlRuleRepositoryTest {
    private fun repository() = SqlRuleRepository(createTestDatabase(), Dispatchers.Unconfined)

    // ---- blocked numbers ----

    @Test
    fun addedBlockedNumberAppearsInBothTheFlowAndTheSnapshot() =
        runTest {
            val repo = repository()

            repo.addBlockedNumber("+34600123456", "Spam caller")

            val fromFlow = repo.blockedNumbers().first().single()
            val fromSnapshot = repo.blockedNumberEntries().single()
            assertEquals("+34600123456", fromFlow.number)
            assertEquals("Spam caller", fromFlow.label)
            assertEquals(fromFlow.id, fromSnapshot.id)
            assertTrue(fromFlow.createdAt > 0, "createdAt should be defaulted by the schema")
        }

    @Test
    fun blockedNumbersAreOrderedMostRecentFirst() =
        runTest {
            val repo = repository()

            repo.addBlockedNumber("+34600000001", null)
            repo.addBlockedNumber("+34600000002", null)
            repo.addBlockedNumber("+34600000003", null)

            // created_at defaults to whole seconds, so ties are broken by id -- the assertion
            // that matters is that the newest row leads.
            assertEquals(
                "+34600000003",
                repo
                    .blockedNumbers()
                    .first()
                    .first()
                    .number,
            )
        }

    @Test
    fun aDuplicateBlockedNumberIsIgnoredRatherThanDuplicated() =
        runTest {
            val repo = repository()

            repo.addBlockedNumber("+34600123456", null)
            repo.addBlockedNumber("+34600123456", "Second attempt")

            assertEquals(1, repo.blockedNumberEntries().size)
        }

    @Test
    fun removeBlockedNumberDeletesOnlyThatRow() =
        runTest {
            val repo = repository()
            repo.addBlockedNumber("+34600000001", null)
            repo.addBlockedNumber("+34600000002", null)
            val target = repo.blockedNumberEntries().first { it.number == "+34600000001" }

            repo.removeBlockedNumber(target.id)

            assertEquals(listOf("+34600000002"), repo.blockedNumberEntries().map { it.number })
        }

    // ---- allowlist ----

    @Test
    fun allowlistedNumbersRoundTripAndDelete() =
        runTest {
            val repo = repository()

            repo.addAllowlistedNumber("+34600999999", "Mom")
            val entry = repo.allowlistedNumbers().first().single()
            assertEquals("+34600999999", entry.number)
            assertEquals("Mom", entry.label)
            assertEquals(entry.id, repo.allowlistedNumberEntries().single().id)

            repo.removeAllowlistedNumber(entry.id)

            assertTrue(repo.allowlistedNumberEntries().isEmpty())
        }

    // ---- pattern rules ----

    @Test
    fun aNewPatternRuleIsEnabled() =
        runTest {
            val repo = repository()

            repo.addPatternRule("+34900*", "Spanish spam")

            val entry = repo.patternRules().first().single()
            assertTrue(entry.enabled)
            assertEquals("+34900*", entry.pattern)
            assertEquals(listOf("+34900*"), repo.enabledPatterns().map { it.pattern })
        }

    @Test
    fun aDisabledPatternRuleStaysListedButLeavesTheEnabledSnapshot() =
        runTest {
            val repo = repository()
            repo.addPatternRule("+34900*", null)
            val id =
                repo
                    .patternRules()
                    .first()
                    .single()
                    .id

            repo.togglePatternRule(id, enabled = false)

            // Still visible in the UI list...
            assertFalse(
                repo
                    .patternRules()
                    .first()
                    .single()
                    .enabled,
            )
            // ...but the rule engine must not see it.
            assertTrue(repo.enabledPatterns().isEmpty())
        }

    @Test
    fun removePatternRuleDeletesIt() =
        runTest {
            val repo = repository()
            repo.addPatternRule("+34900*", null)
            val id =
                repo
                    .patternRules()
                    .first()
                    .single()
                    .id

            repo.removePatternRule(id)

            assertTrue(repo.patternRules().first().isEmpty())
        }

    // ---- country rules ----

    @Test
    fun countryRulesRoundTripAndRespectTheEnabledFlag() =
        runTest {
            val repo = repository()
            repo.addCountryRule("1", "United States")
            val entry = repo.countryRules().first().single()
            assertEquals("1", entry.countryCode)
            assertEquals("United States", entry.countryName)
            assertEquals(1, repo.enabledCountryRules().size)

            repo.toggleCountryRule(entry.id, enabled = false)

            assertTrue(repo.enabledCountryRules().isEmpty())
            assertEquals(1, repo.countryRules().first().size)

            repo.removeCountryRule(entry.id)

            assertTrue(repo.countryRules().first().isEmpty())
        }

    // ---- action rules ----

    @Test
    fun actionRuleFieldsSurviveTheRoundTrip() =
        runTest {
            val repo = repository()

            repo.addActionRule(label = "Repeat caller", attempts = 4, windowMinutes = 15, patternId = null)

            val entry = repo.actionRules().first().single()
            assertEquals("Repeat caller", entry.label)
            assertEquals(4, entry.attempts)
            assertEquals(15, entry.windowMinutes)
            assertEquals(null, entry.patternId)

            val enabled = repo.enabledActionRules().single()
            assertEquals(4, enabled.attempts)
            assertEquals(15, enabled.windowMinutes)
        }

    @Test
    fun anActionRuleCanReferenceAPatternRule() =
        runTest {
            val repo = repository()
            repo.addPatternRule("+34900*", null)
            val patternId =
                repo
                    .patternRules()
                    .first()
                    .single()
                    .id

            // The pattern_id foreign key is the column the migration squash was needed to get
            // right; a bad reference fails the insert rather than silently storing null.
            repo.addActionRule(label = null, attempts = 3, windowMinutes = 5, patternId = patternId)

            assertEquals(patternId, repo.enabledActionRules().single().patternId)
        }

    @Test
    fun aDisabledActionRuleLeavesTheEnabledSnapshot() =
        runTest {
            val repo = repository()
            repo.addActionRule(label = null, attempts = 3, windowMinutes = 5, patternId = null)
            val id =
                repo
                    .actionRules()
                    .first()
                    .single()
                    .id

            repo.toggleActionRule(id, enabled = false)

            assertTrue(repo.enabledActionRules().isEmpty())

            repo.removeActionRule(id)

            assertTrue(repo.actionRules().first().isEmpty())
        }

    // ---- schedule rules ----

    @Test
    fun scheduleRuleMinutesSurviveTheRoundTripIncludingAMidnightCrossingWindow() =
        runTest {
            val repo = repository()

            // 22:00 -> 07:00, i.e. end < start. The resolver treats that as crossing midnight,
            // so the raw values must be stored as given rather than normalised.
            repo.addScheduleRule(label = "Night", startMinute = 1320, endMinute = 420)

            val entry = repo.scheduleRules().first().single()
            assertEquals(1320, entry.startMinute)
            assertEquals(420, entry.endMinute)

            val enabled = repo.enabledScheduleRules().single()
            assertEquals(1320, enabled.startMinute)
            assertEquals(420, enabled.endMinute)
        }

    @Test
    fun aDisabledScheduleRuleLeavesTheEnabledSnapshot() =
        runTest {
            val repo = repository()
            repo.addScheduleRule(label = null, startMinute = 60, endMinute = 120)
            val id =
                repo
                    .scheduleRules()
                    .first()
                    .single()
                    .id

            repo.toggleScheduleRule(id, enabled = false)

            assertTrue(repo.enabledScheduleRules().isEmpty())

            repo.removeScheduleRule(id)

            assertTrue(repo.scheduleRules().first().isEmpty())
        }

    // ---- call attempts (backs both action rules and the repeated-caller bypass) ----

    @Test
    fun countRecentAttemptsCountsOnlyTheGivenNumberInsideTheWindow() =
        runTest {
            val repo = repository()
            val now = 1_000_000_000L
            val hour = 3_600_000L

            repo.recordCallAttempt("+34600000001", now)
            repo.recordCallAttempt("+34600000001", now - hour)
            repo.recordCallAttempt("+34600000001", now - 48 * hour)
            repo.recordCallAttempt("+34600000002", now)

            assertEquals(2, repo.countRecentAttempts("+34600000001", now - 24 * hour))
            assertEquals(3, repo.countRecentAttempts("+34600000001", 0))
            assertEquals(1, repo.countRecentAttempts("+34600000002", now - 24 * hour))
            assertEquals(0, repo.countRecentAttempts("+34600000003", 0))
        }

    @Test
    fun deleteExpiredAttemptsPrunesOnlyOlderRows() =
        runTest {
            val repo = repository()
            val now = 1_000_000_000L
            val hour = 3_600_000L
            repo.recordCallAttempt("+34600000001", now)
            repo.recordCallAttempt("+34600000001", now - 48 * hour)

            repo.deleteExpiredAttempts(now - 24 * hour)

            assertEquals(1, repo.countRecentAttempts("+34600000001", 0))
        }
}
