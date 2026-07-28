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
            assertEquals("spam", decision.blockReason)
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
            assertEquals("Repeated calls (3 in 5m)", decision.blockReason)
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
}
