package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.testing.createTestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The real [SqlCallLogRepository], against a real SQLite engine.
 *
 * Two things only a real database can check: that every [RuleDecision] maps to a `rule_type`
 * the `CallLogEntry` CHECK constraint actually accepts, and that the `strftime`-based stats
 * queries bracket the windows they claim to.
 */
class SqlCallLogRepositoryTest {
    private val now = System.currentTimeMillis()
    private val dayMillis = 86_400_000L

    @Test
    fun logCallPersistsAManualBlockWithItsRuleIdAndLabel() =
        runTest {
            val db = createTestDatabase()
            val repo = SqlCallLogRepository(db)

            repo.logCall("+34600123456", now, RuleDecision.ManualBlock(ruleId = 7, label = "Spam caller"))

            val entry = repo.allEntries().first().single()
            assertEquals("+34600123456", entry.number)
            assertEquals("BLOCKED", entry.action)
            assertEquals("MANUAL", entry.ruleType)
            assertEquals(7L, entry.ruleId)
            assertEquals("Spam caller", entry.ruleDetail)
        }

    @Test
    fun everyDecisionTypeIsAcceptedByTheRuleTypeCheckConstraint() =
        runTest {
            val db = createTestDatabase()
            val repo = SqlCallLogRepository(db)
            val decisions =
                listOf(
                    RuleDecision.Allowlist(ruleId = 1, label = "Mom") to ("ALLOWED" to "CONTACTS"),
                    RuleDecision.ManualBlock(ruleId = 2, label = null) to ("BLOCKED" to "MANUAL"),
                    RuleDecision.PatternBlock(ruleId = 3, pattern = "+34900*", label = null) to ("BLOCKED" to "PATTERN"),
                    RuleDecision.CountryBlock(ruleId = 4, countryCode = "1", countryName = "US") to ("BLOCKED" to "COUNTRY"),
                    RuleDecision.SpamHit(confidence = 0.9f, source = "bundled") to ("BLOCKED" to "SPAM"),
                    RuleDecision.ActionBlock(ruleId = 5, label = null, attempts = 3, windowMinutes = 5) to ("BLOCKED" to "ACTION"),
                    RuleDecision.ScheduleBlock(ruleId = 6, label = null) to ("BLOCKED" to "SCHEDULE"),
                    RuleDecision.DefaultAllow to ("ALLOWED" to null),
                    RuleDecision.DefaultBlock to ("BLOCKED" to null),
                    RuleDecision.PendingReview to ("ALLOWED" to "REVIEW"),
                    RuleDecision.AllowedAfterRepeatedAttempts(attempts = 3) to ("ALLOWED" to "REPEATED_ALLOWED"),
                )

            decisions.forEachIndexed { index, (decision, _) ->
                repo.logCall("+3460000000$index", now + index, decision)
            }

            val stored = repo.allEntries().first().sortedBy { it.timestamp }
            assertEquals(decisions.size, stored.size)
            decisions.forEachIndexed { index, (_, expected) ->
                val (action, ruleType) = expected
                assertEquals(action, stored[index].action, "action for decision #$index")
                assertEquals(ruleType, stored[index].ruleType, "ruleType for decision #$index")
            }
        }

    @Test
    fun spamHitAndDefaultBlockCarryNoRuleId() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase())

            repo.logCall("+34600000001", now, RuleDecision.SpamHit(confidence = 0.8f, source = "bundled"))
            repo.logCall("+34600000002", now + 1, RuleDecision.DefaultBlock)

            // Neither decision came from a rule row, so there is no id to persist.
            assertTrue(repo.allEntries().first().all { it.ruleId == null })
        }

    @Test
    fun allEntriesAreOrderedMostRecentFirst() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase())

            repo.logCall("+34600000001", now - 2 * dayMillis, RuleDecision.DefaultBlock)
            repo.logCall("+34600000002", now, RuleDecision.DefaultBlock)
            repo.logCall("+34600000003", now - dayMillis, RuleDecision.DefaultBlock)

            val numbers = repo.allEntries().first().map { it.number }
            assertEquals(listOf("+34600000002", "+34600000003", "+34600000001"), numbers)
        }

    @Test
    fun recentEntriesTruncatesToTheRequestedLimit() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase())
            repeat(5) { repo.logCall("+3460000000$it", now - it * 1000L, RuleDecision.DefaultBlock) }

            val recent = repo.recentEntries(limit = 2).first()

            assertEquals(2, recent.size)
            assertEquals("+34600000000", recent.first().number)
        }

    @Test
    fun blockedStatsCountOnlyBlockedCallsInsideTheirWindow() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase())

            repo.logCall("+34600000001", now, RuleDecision.DefaultBlock)
            // 40 days is older than any calendar month, so it falls outside every window.
            repo.logCall("+34600000002", now - 40 * dayMillis, RuleDecision.DefaultBlock)
            repo.logCall("+34600000003", now, RuleDecision.DefaultAllow)
            repo.logCall("+34600000004", now, RuleDecision.PendingReview)

            val stats = repo.blockedStats()

            assertEquals(1, stats.today)
            assertEquals(1, stats.thisWeek)
            assertEquals(1, stats.thisMonth)
            // pendingReview is a durable tag, not a time window — the ALLOWED review entry counts.
            assertEquals(1, stats.pendingReview)
        }

    @Test
    fun individualCountQueriesAgreeWithBatchedStats() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase())
            repo.logCall("+34600000001", now, RuleDecision.DefaultBlock)
            repo.logCall("+34600000002", now - 40 * dayMillis, RuleDecision.DefaultBlock)

            val stats = repo.blockedStats()

            assertEquals(stats.today, repo.blockedCountToday())
            assertEquals(stats.thisWeek, repo.blockedCountThisWeek())
            assertEquals(stats.thisMonth, repo.blockedCountThisMonth())
        }

    @Test
    fun clearAllEmptiesTheLog() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase())
            repo.logCall("+34600000001", now, RuleDecision.DefaultBlock)

            repo.clearAll()

            assertTrue(repo.allEntries().first().isEmpty())
            assertEquals(0, repo.blockedStats().today)
        }

    @Test
    fun blockedByDayReturnsOneBucketPerRequestedDay() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase())

            val stats = repo.blockedByDay(daysBack = 7)

            assertEquals(7, stats.size)
            // Reversed on the way out: newest bucket first.
            assertTrue(stats.first().cutoffEpochMillis > stats.last().cutoffEpochMillis)
        }

    @Test
    fun blockedByDayBucketsAreRolling24hWindowsAnchoredAtNow() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase())
            repo.logCall("+34600000001", now - 1000L, RuleDecision.DefaultBlock)
            repo.logCall("+34600000002", now - 2 * dayMillis, RuleDecision.DefaultBlock)
            repo.logCall("+34600000003", now - 30 * dayMillis, RuleDecision.DefaultBlock)

            val stats = repo.blockedByDay(daysBack = 7)

            // A call one second old lands in the newest bucket; one two days old, two buckets
            // back; one 30 days old falls outside the range entirely.
            assertEquals(1, stats[0].count)
            assertEquals(1, stats[2].count)
            assertEquals(2, stats.sumOf { it.count })
        }

    @Test
    fun blockedByDayLabelsTheNewestBucketYesterday() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase())
            repo.logCall("+34600000001", now - 1000L, RuleDecision.DefaultBlock)

            val stats = repo.blockedByDay(daysBack = 7)

            // Documents current behaviour, which reads as a labelling bug: buckets are
            // rolling 24h windows starting at now-7d, so the newest one spans [now-1d, now)
            // and holds calls from the last 24 hours -- including one a second old -- yet
            // its label is derived from its *start*, making it "Yesterday". No bucket is
            // ever labelled "Today". StatsScreen renders these labels verbatim.
            assertEquals("Yesterday", stats.first().dateLabel)
            assertEquals(1, stats.first().count)
            assertTrue(stats.none { it.dateLabel == "Today" })
        }
}
