package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RulePrecedenceResolverTest {
    private val emptyContext =
        ResolveContext(
            allowlistedNumbers = emptySet(),
            blockedNumbers = emptySet(),
            enabledPatterns = emptyList(),
            enabledCountryCodes = emptySet(),
        )

    @Test
    fun noRules_defaultsToAllow() =
        runTest {
            val decision = RulePrecedenceResolver.evaluate("+34600123456", emptyContext)
            assertTrue(decision is RuleDecision.DefaultAllow)
            assertFalse(decision.isBlocked)
        }

    @Test
    fun manualBlock_matchesExactNumber() =
        runTest {
            val ctx = emptyContext.copy(blockedNumbers = setOf("+34600123456"))
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.ManualBlock)
            assertTrue(decision.isBlocked)
        }

    @Test
    fun manualBlock_doesNotMatchDifferentNumber() =
        runTest {
            val ctx = emptyContext.copy(blockedNumbers = setOf("+34600123456"))
            val decision = RulePrecedenceResolver.evaluate("+34600999999", ctx)
            assertTrue(decision is RuleDecision.DefaultAllow)
        }

    @Test
    fun manualBlock_overridesAllowlist() =
        runTest {
            val ctx =
                emptyContext.copy(
                    allowlistedNumbers = setOf("+34600123456"),
                    blockedNumbers = setOf("+34600123456"),
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.ManualBlock)
            assertTrue(decision.isBlocked)
        }

    @Test
    fun manualBlock_overridesContacts() =
        runTest {
            val ctx =
                emptyContext.copy(
                    contactNumbers = setOf("+34600123456"),
                    blockedNumbers = setOf("+34600123456"),
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.ManualBlock)
            assertTrue(decision.isBlocked)
        }

    @Test
    fun allowlistStillWorksWithoutManualBlock() =
        runTest {
            val ctx =
                emptyContext.copy(
                    allowlistedNumbers = setOf("+34600123456"),
                    blockedNumbers = setOf("+34600999999"),
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.Allowlist)
            assertFalse(decision.isBlocked)
        }

    @Test
    fun patternMatch_prefixGlob() =
        runTest {
            val ctx =
                emptyContext.copy(
                    enabledPatterns =
                        listOf(
                            PatternRule(id = 1, pattern = "+34900*", label = "Spanish spam", enabled = true),
                        ),
                )
            val decision = RulePrecedenceResolver.evaluate("+34900123456", ctx)
            assertTrue(decision is RuleDecision.PatternBlock)
            assertEquals(1L, (decision as RuleDecision.PatternBlock).ruleId)
        }

    @Test
    fun patternMatch_containsGlob() =
        runTest {
            val ctx =
                emptyContext.copy(
                    enabledPatterns =
                        listOf(
                            PatternRule(id = 2, pattern = "*900*", label = null, enabled = true),
                        ),
                )
            assertTrue(RulePrecedenceResolver.evaluate("+34900123456", ctx) is RuleDecision.PatternBlock)
            assertTrue(RulePrecedenceResolver.evaluate("0034900999", ctx) is RuleDecision.PatternBlock)
            assertFalse(RulePrecedenceResolver.evaluate("+34600123", ctx).isBlocked)
        }

    @Test
    fun patternMatch_exact() =
        runTest {
            val ctx =
                emptyContext.copy(
                    enabledPatterns =
                        listOf(
                            PatternRule(id = 3, pattern = "+34900", label = null, enabled = true),
                        ),
                )
            assertTrue(RulePrecedenceResolver.evaluate("+34900", ctx) is RuleDecision.PatternBlock)
            assertFalse(RulePrecedenceResolver.evaluate("+349001", ctx).isBlocked)
        }

    @Test
    fun patternMatch_caseInsensitive() =
        runTest {
            val ctx =
                emptyContext.copy(
                    enabledPatterns =
                        listOf(
                            PatternRule(id = 4, pattern = "ABC*", label = null, enabled = true),
                        ),
                )
            assertTrue(RulePrecedenceResolver.evaluate("abc123", ctx) is RuleDecision.PatternBlock)
            assertTrue(RulePrecedenceResolver.evaluate("ABC123", ctx) is RuleDecision.PatternBlock)
        }

    @Test
    fun countryBlock_matchesParsedCode() =
        runTest {
            val ctx = emptyContext.copy(enabledCountryCodes = setOf("34"))
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.CountryBlock)
            assertEquals("34", (decision as RuleDecision.CountryBlock).countryCode)
        }

    @Test
    fun countryBlock_doesNotMatchDifferentCode() =
        runTest {
            val ctx = emptyContext.copy(enabledCountryCodes = setOf("34"))
            val decision = RulePrecedenceResolver.evaluate("+442012345678", ctx)
            assertFalse(decision.isBlocked)
        }

    @Test
    fun precedence_allowlistBeatsPattern() =
        runTest {
            val ctx =
                emptyContext.copy(
                    allowlistedNumbers = setOf("+34900123456"),
                    enabledPatterns =
                        listOf(PatternRule(id = 1, pattern = "+34900*", label = null, enabled = true)),
                )
            assertTrue(RulePrecedenceResolver.evaluate("+34900123456", ctx) is RuleDecision.Allowlist)
        }

    @Test
    fun precedence_manualBlockBeatsPattern() =
        runTest {
            val ctx =
                emptyContext.copy(
                    blockedNumbers = setOf("+34900123456"),
                    enabledPatterns =
                        listOf(PatternRule(id = 1, pattern = "+34900*", label = null, enabled = true)),
                )
            assertTrue(RulePrecedenceResolver.evaluate("+34900123456", ctx) is RuleDecision.ManualBlock)
        }

    @Test
    fun precedence_patternBeatsCountry() =
        runTest {
            val ctx =
                emptyContext.copy(
                    enabledPatterns =
                        listOf(PatternRule(id = 1, pattern = "+34600*", label = null, enabled = true)),
                    enabledCountryCodes = setOf("34"),
                )
            assertTrue(RulePrecedenceResolver.evaluate("+34600123456", ctx) is RuleDecision.PatternBlock)
        }

    @Test
    fun noEnabledPatterns_doesNotBlock() =
        runTest {
            val ctx =
                emptyContext.copy(
                    enabledPatterns = emptyList(),
                )
            assertFalse(RulePrecedenceResolver.evaluate("+34900123456", ctx).isBlocked)
        }

    @Test
    fun disabledCountry_doesNotBlock() =
        runTest {
            val ctx = emptyContext.copy(enabledCountryCodes = emptySet())
            assertFalse(RulePrecedenceResolver.evaluate("+34600123456", ctx).isBlocked)
        }

    @Test
    fun blockReason_isHumanReadable() =
        runTest {
            val ctx = emptyContext.copy(blockedNumbers = setOf("+34600123456"))
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertEquals("Manually blocked", decision.blockReason)
        }

    @Test
    fun blockReason_patternIncludesPattern() =
        runTest {
            val ctx =
                emptyContext.copy(
                    enabledPatterns =
                        listOf(PatternRule(id = 1, pattern = "+34900*", label = "Spam prefix", enabled = true)),
                )
            val decision = RulePrecedenceResolver.evaluate("+34900123456", ctx)
            assertEquals("Spam prefix", decision.blockReason)
        }

    @Test
    fun patternMatch_suffixGlob() =
        runTest {
            val ctx =
                emptyContext.copy(
                    enabledPatterns =
                        listOf(PatternRule(id = 1, pattern = "*1234", label = null, enabled = true)),
                )
            assertTrue(RulePrecedenceResolver.evaluate("+349001234", ctx) is RuleDecision.PatternBlock)
            assertFalse(RulePrecedenceResolver.evaluate("+349001235", ctx).isBlocked)
        }

    @Test
    fun emptyPattern_doesNotBlock() =
        runTest {
            val ctx =
                emptyContext.copy(
                    enabledPatterns =
                        listOf(PatternRule(id = 1, pattern = "", label = null, enabled = true)),
                )
            assertFalse(RulePrecedenceResolver.evaluate("+34600123456", ctx).isBlocked)
        }

    @Test
    fun whitespaceIsTrimmed() =
        runTest {
            val ctx = emptyContext.copy(blockedNumbers = setOf("+34600123456"))
            val decision = RulePrecedenceResolver.evaluate("  +34600123456  ", ctx)
            assertTrue(decision is RuleDecision.ManualBlock)
        }

    @Test
    fun matchesPattern_prefixOnly() {
        assertTrue(RulePrecedenceResolver.matchesPattern("+34900123", "+34900*"))
        assertFalse(RulePrecedenceResolver.matchesPattern("+34600123", "+34900*"))
    }

    @Test
    fun matchesPattern_emptyPatternReturnsFalse() {
        assertFalse(RulePrecedenceResolver.matchesPattern("+34600123", ""))
    }

    @Test
    fun manualBlock_overridesContacts_legacyCase() =
        runTest {
            val ctx =
                emptyContext.copy(
                    contactNumbers = setOf("+34600123456"),
                    blockedNumbers = setOf("+34600123456"),
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.ManualBlock)
            assertTrue(decision.isBlocked)
        }

    @Test
    fun contacts_overridesPattern() =
        runTest {
            val ctx =
                emptyContext.copy(
                    contactNumbers = setOf("+34900123456"),
                    enabledPatterns = listOf(PatternRule(id = 1, pattern = "+34900*", label = null, enabled = true)),
                )
            assertTrue(RulePrecedenceResolver.evaluate("+34900123456", ctx) is RuleDecision.Allowlist)
        }

    @Test
    fun contacts_emptySet_doesNotAffectOutcome() =
        runTest {
            val ctx = emptyContext.copy(blockedNumbers = setOf("+34600123456"))
            assertTrue(RulePrecedenceResolver.evaluate("+34600123456", ctx) is RuleDecision.ManualBlock)
        }

    @Test
    fun contacts_combinedWithAllowlist() =
        runTest {
            val ctx =
                emptyContext.copy(
                    contactNumbers = setOf("+34900123456"),
                    allowlistedNumbers = setOf("+34900999999"),
                )
            assertTrue(RulePrecedenceResolver.evaluate("+34900123456", ctx) is RuleDecision.Allowlist)
            assertTrue(RulePrecedenceResolver.evaluate("+34900999999", ctx) is RuleDecision.Allowlist)
            assertFalse(RulePrecedenceResolver.evaluate("+34900111111", ctx).isBlocked)
        }
}
