package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.testing.createTestDatabase
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    fun logCallReturnsTheIdOfTheRowItJustWrote() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)

            val first = repo.logCall("+34600000001", now, RuleDecision.DefaultBlock)
            val second = repo.logCall("+34600000002", now + 1, RuleDecision.DefaultBlock)

            // The recorder attaches audio by this id after the call has ended, so a wrong id
            // silently files a caller's recording under a different caller's log entry.
            assertEquals(
                listOf(first, second),
                repo
                    .allEntries()
                    .first()
                    .map { it.id }
                    .sorted(),
            )
            assertTrue(second > first)
        }

    @Test
    fun attachRecordingPointsTheEntryAtTheFile() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)
            val id = repo.logCall("+34600123456", now, RuleDecision.DefaultBlock)
            assertNull(
                repo
                    .allEntries()
                    .first()
                    .single()
                    .recordingPath,
            )

            repo.attachRecording(id, "/data/app/recordings/call_$id.m4a")

            assertEquals(
                "/data/app/recordings/call_$id.m4a",
                repo
                    .allEntries()
                    .first()
                    .single()
                    .recordingPath,
            )
        }

    @Test
    fun deleteRecordingClearsThePathEvenWhenTheFileIsAlreadyGone() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)
            val id = repo.logCall("+34600123456", now, RuleDecision.DefaultBlock)
            // A path that was never written: RecordingStore.delete treats a missing file as
            // success, so the column must still end up null rather than stranding the UI with a
            // play button for audio the user asked to destroy.
            repo.attachRecording(id, "/nonexistent/call_$id.m4a")

            repo.deleteRecording(id)

            assertNull(
                repo
                    .allEntries()
                    .first()
                    .single()
                    .recordingPath,
            )
        }

    @Test
    fun deleteRecordingOnAnEntryWithNoRecordingIsANoOp() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)
            val id = repo.logCall("+34600123456", now, RuleDecision.DefaultBlock)

            repo.deleteRecording(id)

            assertEquals(1, repo.allEntries().first().size)
            assertNull(
                repo
                    .allEntries()
                    .first()
                    .single()
                    .recordingPath,
            )
        }

    @Test
    fun clearAllDeletesTheRecordingFilesAndNotJustTheRows() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)
            val file = File.createTempFile("corta-spam-recording", ".m4a")
            val id = repo.logCall("+34600123456", now, RuleDecision.DefaultBlock)
            repo.attachRecording(id, file.absolutePath)
            assertTrue(file.exists())

            repo.clearAll()

            // The rows are what the UI reads, so a surviving file is invisible -- audio of a
            // real person's voice left on disk after the user pressed "clear log".
            assertTrue(repo.allEntries().first().isEmpty())
            assertFalse(file.exists())
        }

    @Test
    fun logCallPersistsAManualBlockWithItsRuleIdAndLabel() =
        runTest {
            val db = createTestDatabase()
            val repo = SqlCallLogRepository(db, Dispatchers.Unconfined)

            repo.logCall("+34600123456", now, RuleDecision.ManualBlock(ruleId = 7, label = "Spam caller"))

            val entry = repo.allEntries().first().single()
            assertEquals("+34600123456", entry.number)
            assertEquals("BLOCKED", entry.action)
            assertEquals("MANUAL", entry.ruleType)
            assertEquals(7L, entry.ruleId)
            // rule_detail holds the reason as data, not as an English sentence, so the call log
            // re-renders in whatever locale the reader is using rather than the one it was
            // written in. The user's own label still round-trips verbatim inside it.
            assertEquals(BlockReason.Custom("Spam caller"), BlockReasonCodec.decode(entry.ruleDetail))
        }

    @Test
    fun everyDecisionTypeIsAcceptedByTheRuleTypeCheckConstraint() =
        runTest {
            val db = createTestDatabase()
            val repo = SqlCallLogRepository(db, Dispatchers.Unconfined)
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
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)

            repo.logCall("+34600000001", now, RuleDecision.SpamHit(confidence = 0.8f, source = "bundled"))
            repo.logCall("+34600000002", now + 1, RuleDecision.DefaultBlock)

            // Neither decision came from a rule row, so there is no id to persist.
            assertTrue(repo.allEntries().first().all { it.ruleId == null })
        }

    @Test
    fun allEntriesAreOrderedMostRecentFirst() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)

            repo.logCall("+34600000001", now - 2 * dayMillis, RuleDecision.DefaultBlock)
            repo.logCall("+34600000002", now, RuleDecision.DefaultBlock)
            repo.logCall("+34600000003", now - dayMillis, RuleDecision.DefaultBlock)

            val numbers = repo.allEntries().first().map { it.number }
            assertEquals(listOf("+34600000002", "+34600000003", "+34600000001"), numbers)
        }

    @Test
    fun recentEntriesTruncatesToTheRequestedLimit() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)
            repeat(5) { repo.logCall("+3460000000$it", now - it * 1000L, RuleDecision.DefaultBlock) }

            val recent = repo.recentEntries(limit = 2).first()

            assertEquals(2, recent.size)
            assertEquals("+34600000000", recent.first().number)
        }

    @Test
    fun blockedStatsCountOnlyBlockedCallsInsideTheirWindow() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)

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
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)
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
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)
            repo.logCall("+34600000001", now, RuleDecision.DefaultBlock)

            repo.clearAll()

            assertTrue(repo.allEntries().first().isEmpty())
            assertEquals(0, repo.blockedStats().today)
        }

    @Test
    fun blockedByDayReturnsOneBucketPerRequestedDay() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)

            val stats = repo.blockedByDay(daysBack = 7)

            assertEquals(7, stats.size)
            // Reversed on the way out: newest bucket first.
            assertTrue(stats.first().cutoffEpochMillis > stats.last().cutoffEpochMillis)
        }

    @Test
    fun blockedByDayBucketsAreCalendarDays() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)
            repo.logCall("+34600000001", now, RuleDecision.DefaultBlock)
            repo.logCall("+34600000002", now - 2 * dayMillis, RuleDecision.DefaultBlock)
            repo.logCall("+34600000003", now - 30 * dayMillis, RuleDecision.DefaultBlock)

            val stats = repo.blockedByDay(daysBack = 7)

            // A call made now lands in today's bucket; one two days old, two buckets back;
            // one 30 days old falls outside the range entirely.
            assertEquals(1, stats[0].count)
            assertEquals(1, stats[2].count)
            assertEquals(2, stats.sumOf { it.count })
        }

    @Test
    fun blockedByDayNumbersTheNewestBucketAsToday() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)
            repo.logCall("+34600000001", now, RuleDecision.DefaultBlock)
            repo.logCall("+34600000002", now - dayMillis, RuleDecision.DefaultBlock)

            val stats = repo.blockedByDay(daysBack = 7)

            // The bucket carries how many days back it is; the *words* "Today"/"Yesterday"
            // are chosen at the presentation edge, where the user's locale is available.
            assertEquals(0, stats[0].daysAgo)
            assertEquals(1, stats[0].count)
            assertEquals(1, stats[1].daysAgo)
            assertEquals(1, stats[1].count)
            assertEquals(List(7) { it }, stats.map { it.daysAgo }, "newest bucket first, one per day")
        }

    @Test
    fun todaysBucketAgreesWithBlockedStatsToday() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)
            repo.logCall("+34600000001", now, RuleDecision.DefaultBlock)
            repo.logCall("+34600000002", now, RuleDecision.DefaultBlock)
            repo.logCall("+34600000003", now - dayMillis, RuleDecision.DefaultBlock)

            // Both align to UTC midnight, so the chart's first bar and the "blocked today"
            // stat can never disagree. Anchoring buckets at `now` used to break this.
            assertEquals(repo.blockedStats().today, repo.blockedByDay(daysBack = 7).first().count)
        }

    /**
     * The direction column against the real CHECK constraint. A value the constraint rejects
     * fails here and nowhere else — the Kotlin enum compiles whatever it is named.
     */
    @Test
    fun outgoingCallsAreLoggedWithTheirDirectionAndNoRule() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)

            repo.logOutgoingCall("+34600123456", now)

            val entry = repo.allEntries().first().single()
            assertEquals(CallDirection.OUTGOING, entry.direction)
            assertEquals("+34600123456", entry.number)
            // No decision was made, so nothing may be reported as one.
            assertEquals("ALLOWED", entry.action)
            assertNull(entry.ruleType)
            assertNull(entry.ruleId)
            assertNull(entry.ruleDetail)
        }

    @Test
    fun screenedCallsStayIncoming() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)

            repo.logCall("+34600123456", now, RuleDecision.DefaultBlock)

            assertEquals(
                CallDirection.INCOMING,
                repo
                    .allEntries()
                    .first()
                    .single()
                    .direction,
            )
        }

    /**
     * A call the user placed must never move the blocked-call counters. It is written as ALLOWED
     * precisely so it cannot, and this is the assertion that keeps that true if the value ever
     * changes.
     */
    @Test
    fun outgoingCallsDoNotCountAsBlocked() =
        runTest {
            val repo = SqlCallLogRepository(createTestDatabase(), Dispatchers.Unconfined)

            repo.logOutgoingCall("+34600123456", now)
            repo.logOutgoingCall("+34600999999", now)

            assertEquals(0, repo.blockedStats().today)
            assertEquals(0, repo.blockedCountThisMonth())
        }
}
