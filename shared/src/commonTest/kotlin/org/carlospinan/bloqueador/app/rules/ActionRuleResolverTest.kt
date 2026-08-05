package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActionRuleResolverTest {
    private val emptyContext =
        ResolveContext(
            allowlistedNumbers = emptySet(),
            blockedNumbers = emptySet(),
            enabledPatterns = emptyList(),
            enabledCountryCodes = emptySet(),
        )

    @Test
    fun underThreshold_allows() =
        runTest {
            val rule = ActionRule(id = 1, label = "spam", attempts = 3, windowMinutes = 5, patternId = null, enabled = true)
            val ctx =
                emptyContext.copy(
                    enabledActionRules = listOf(rule),
                    attemptCountsByWindowMinutes = mapOf(5 to 2),
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.DefaultAllow)
            assertFalse(decision.isBlocked)
        }

    @Test
    fun atThreshold_blocks() =
        runTest {
            val rule = ActionRule(id = 1, label = "spam", attempts = 3, windowMinutes = 5, patternId = null, enabled = true)
            val ctx =
                emptyContext.copy(
                    enabledActionRules = listOf(rule),
                    attemptCountsByWindowMinutes = mapOf(5 to 3),
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.ActionBlock)
            assertTrue(decision.isBlocked)
            assertEquals("ACTION", decision.ruleTypeTag)
            assertEquals(BlockReason.Custom("spam"), decision.reason)
        }

    @Test
    fun overThreshold_blocks() =
        runTest {
            val rule = ActionRule(id = 2, label = null, attempts = 3, windowMinutes = 5, patternId = null, enabled = true)
            val ctx =
                emptyContext.copy(
                    enabledActionRules = listOf(rule),
                    attemptCountsByWindowMinutes = mapOf(5 to 10),
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.ActionBlock)
            // Structured, not a sentence -- the wording is chosen per locale at display time.
            assertEquals(BlockReason.RepeatedCalls(attempts = 3, windowMinutes = 5), decision.reason)
        }

    @Test
    fun differentWindow_usesOwnCount() =
        runTest {
            val rule = ActionRule(id = 1, label = null, attempts = 3, windowMinutes = 10, patternId = null, enabled = true)
            val ctx =
                emptyContext.copy(
                    enabledActionRules = listOf(rule),
                    attemptCountsByWindowMinutes = mapOf(5 to 10, 10 to 1),
                )
            assertTrue(RulePrecedenceResolver.evaluate("+34600123456", ctx) is RuleDecision.DefaultAllow)
        }

    @Test
    fun allowlist_beatsAction() =
        runTest {
            val rule = ActionRule(id = 1, label = null, attempts = 1, windowMinutes = 5, patternId = null, enabled = true)
            val ctx =
                emptyContext.copy(
                    allowlistedNumbers = setOf("+34600123456"),
                    enabledActionRules = listOf(rule),
                    attemptCountsByWindowMinutes = mapOf(5 to 5),
                )
            assertTrue(RulePrecedenceResolver.evaluate("+34600123456", ctx) is RuleDecision.Allowlist)
        }

    @Test
    fun manualBlock_beatsAction() =
        runTest {
            val rule = ActionRule(id = 1, label = null, attempts = 1, windowMinutes = 5, patternId = null, enabled = true)
            val ctx =
                emptyContext.copy(
                    blockedNumbers = setOf("+34600123456"),
                    enabledActionRules = listOf(rule),
                    attemptCountsByWindowMinutes = mapOf(5 to 5),
                )
            assertTrue(RulePrecedenceResolver.evaluate("+34600123456", ctx) is RuleDecision.ManualBlock)
        }

    @Test
    fun noActionRules_allows() =
        runTest {
            val ctx = emptyContext.copy(attemptCountsByWindowMinutes = mapOf(5 to 100))
            assertTrue(RulePrecedenceResolver.evaluate("+34600123456", ctx) is RuleDecision.DefaultAllow)
        }

    @Test
    fun firstMatchingActionRule_wins() =
        runTest {
            val rules =
                listOf(
                    ActionRule(id = 1, label = "first", attempts = 2, windowMinutes = 5, patternId = null, enabled = true),
                    ActionRule(id = 2, label = "second", attempts = 2, windowMinutes = 5, patternId = null, enabled = true),
                )
            val ctx =
                emptyContext.copy(
                    enabledActionRules = rules,
                    attemptCountsByWindowMinutes = mapOf(5 to 2),
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.ActionBlock)
            assertEquals(1L, (decision as RuleDecision.ActionBlock).ruleId)
        }

    // ---- patternId scoping ----
    //
    // patternId narrows *which callers a rule counts*, so it is resolved against every pattern,
    // not just the enabled ones. Scoping to an enabled pattern is self-defeating: step 3 returns
    // a PatternBlock for those numbers, so step 6 never sees them. Looking the scope up in
    // `enabledPatterns` therefore made the entire branch unreachable — any rule with a patternId
    // set was dead code, whatever its threshold.

    @Test
    fun patternScopedRule_firesForANumberMatchingItsDisabledScopePattern() =
        runTest {
            val scope = PatternRule(id = 7, pattern = "+34900*", label = "Spanish 900s", enabled = false)
            val ctx =
                emptyContext.copy(
                    allPatterns = listOf(scope),
                    enabledActionRules =
                        listOf(ActionRule(id = 1, label = "900 repeats", attempts = 2, windowMinutes = 5, patternId = 7, enabled = true)),
                    attemptCountsByWindowMinutes = mapOf(5 to 3),
                )

            val decision = RulePrecedenceResolver.evaluate("+34900123456", ctx)

            assertTrue(decision is RuleDecision.ActionBlock)
            assertEquals(1L, decision.ruleId)
        }

    @Test
    fun patternScopedRule_doesNotFireForANumberOutsideItsScope() =
        runTest {
            val scope = PatternRule(id = 7, pattern = "+34900*", label = null, enabled = false)
            val ctx =
                emptyContext.copy(
                    allPatterns = listOf(scope),
                    enabledActionRules =
                        listOf(ActionRule(id = 1, label = null, attempts = 2, windowMinutes = 5, patternId = 7, enabled = true)),
                    attemptCountsByWindowMinutes = mapOf(5 to 99),
                )

            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)

            assertTrue(decision is RuleDecision.DefaultAllow)
        }

    @Test
    fun patternScopedRule_withAnUnresolvableScopeDoesNotFire() =
        runTest {
            val ctx =
                emptyContext.copy(
                    allPatterns = emptyList(),
                    enabledActionRules =
                        listOf(ActionRule(id = 1, label = null, attempts = 2, windowMinutes = 5, patternId = 404, enabled = true)),
                    attemptCountsByWindowMinutes = mapOf(5 to 99),
                )

            // Fail open: a scope we can't resolve means we don't know who the rule was for, and
            // guessing "everyone" would block the whole phone.
            assertTrue(RulePrecedenceResolver.evaluate("+34900123456", ctx) is RuleDecision.DefaultAllow)
        }

    @Test
    fun anEnabledScopePatternStillBlocksAtStepThreeFirst() =
        runTest {
            val scope = PatternRule(id = 7, pattern = "+34900*", label = null, enabled = true)
            val ctx =
                emptyContext.copy(
                    enabledPatterns = listOf(scope),
                    allPatterns = listOf(scope),
                    enabledActionRules =
                        listOf(ActionRule(id = 1, label = null, attempts = 2, windowMinutes = 5, patternId = 7, enabled = true)),
                    attemptCountsByWindowMinutes = mapOf(5 to 99),
                )

            // Documented, not a bug: the pattern already blocks these numbers outright, so the
            // action rule has nothing left to do. This is why scopes are useful only when the
            // pattern is disabled as a block rule.
            assertTrue(RulePrecedenceResolver.evaluate("+34900123456", ctx) is RuleDecision.PatternBlock)
        }
}
