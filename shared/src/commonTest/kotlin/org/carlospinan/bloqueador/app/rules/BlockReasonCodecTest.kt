package org.carlospinan.bloqueador.app.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The call log stores *why* a call was blocked, and must survive being read back later.
 *
 * The column used to hold an English sentence built at block time. That made the log
 * untranslatable after the fact — a Spanish user's history stayed in English forever — and it
 * meant nothing downstream could reason about the cause. These tests pin both directions of the
 * encoding and, importantly, that a row written by an older or newer build still displays.
 */
class BlockReasonCodecTest {
    @Test
    fun everyReasonSurvivesARoundTrip() {
        val reasons =
            listOf(
                BlockReason.Custom("Mum's old landline"),
                BlockReason.ManuallyBlocked,
                BlockReason.PatternMatch("+34900*"),
                BlockReason.Country(countryCode = "34", countryName = "Spain"),
                BlockReason.Spam(source = "bundled", confidencePercent = 70),
                BlockReason.RepeatedCalls(attempts = 3, windowMinutes = 5),
                BlockReason.QuietHours,
                BlockReason.NoMatchingRule,
                BlockReason.AllowedAfterRepeatedAttempts(attempts = 4),
            )

        for (reason in reasons) {
            assertEquals(reason, BlockReasonCodec.decode(BlockReasonCodec.encode(reason)), "round trip failed for $reason")
        }
    }

    @Test
    fun labelsContainingTheSeparatorCharactersSurvive() {
        // Why this is JSON and not "kind:arg:arg" -- labels are free text the user typed, and a
        // colon, quote or brace in one would have silently truncated or corrupted the row.
        val awkward = BlockReason.Custom("""Work: "the office" {main} | ext:42, 100%""")

        assertEquals(awkward, BlockReasonCodec.decode(BlockReasonCodec.encode(awkward)))
    }

    @Test
    fun aPatternContainingPunctuationSurvives() {
        val reason = BlockReason.PatternMatch("""*"900"*""")

        assertEquals(reason, BlockReasonCodec.decode(BlockReasonCodec.encode(reason)))
    }

    @Test
    fun anEmptyOrAbsentDetailDecodesToNothing() {
        assertNull(BlockReasonCodec.decode(null))
        assertNull(BlockReasonCodec.decode(""))
        assertNull(BlockReasonCodec.decode("   "))
    }

    @Test
    fun aRowFromAnOlderBuildStillShowsItsOriginalText() {
        // Pre-existing rows hold a plain English sentence. Showing it verbatim is worse than a
        // translation but far better than a blank cell or a crash while scrolling the log.
        val legacy = BlockReasonCodec.decode("Country: Spain (34)")

        assertEquals(BlockReason.Custom("Country: Spain (34)"), legacy)
    }

    @Test
    fun aReasonKindThisBuildDoesNotKnowDegradesInsteadOfThrowing() {
        val fromTheFuture = """{"type":"quantum_entanglement","spookiness":9}"""

        val decoded = BlockReasonCodec.decode(fromTheFuture)

        assertTrue(decoded is BlockReason.Custom)
        assertEquals(fromTheFuture, decoded.label)
    }

    @Test
    fun theDecisionItselfProducesTheStoredForm() {
        val decision = RuleDecision.CountryBlock(ruleId = 1, countryCode = "34", countryName = "Spain")

        assertEquals(
            BlockReason.Country(countryCode = "34", countryName = "Spain"),
            BlockReasonCodec.decode(decision.loggedDetail),
        )
    }

    @Test
    fun anAllowedCallRecordsNoReason() {
        assertNull(RuleDecision.DefaultAllow.loggedDetail)
        // PendingReview is tagged REVIEW in rule_type; there's no extra sentence to add.
        assertNull(RuleDecision.PendingReview.loggedDetail)
    }
}
