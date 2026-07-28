package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleRuleResolverTest {
    private val emptyContext =
        ResolveContext(
            allowlistedNumbers = emptySet(),
            blockedNumbers = emptySet(),
            enabledPatterns = emptyList(),
            enabledCountryCodes = emptySet(),
        )

    // --- isWithinWindow: pure boundary/wraparound logic ---

    @Test
    fun nonWrappingWindow_insideRange() {
        assertTrue(RulePrecedenceResolver.isWithinWindow(minute = 600, startMinute = 540, endMinute = 660))
    }

    @Test
    fun nonWrappingWindow_beforeStart() {
        assertFalse(RulePrecedenceResolver.isWithinWindow(minute = 500, startMinute = 540, endMinute = 660))
    }

    @Test
    fun nonWrappingWindow_atStart_isInclusive() {
        assertTrue(RulePrecedenceResolver.isWithinWindow(minute = 540, startMinute = 540, endMinute = 660))
    }

    @Test
    fun nonWrappingWindow_atEnd_isExclusive() {
        assertFalse(RulePrecedenceResolver.isWithinWindow(minute = 660, startMinute = 540, endMinute = 660))
    }

    @Test
    fun midnightCrossingWindow_lateNightSide() {
        // 22:00 (1320) - 07:00 (420): 23:30 (1410) should match.
        assertTrue(RulePrecedenceResolver.isWithinWindow(minute = 1410, startMinute = 1320, endMinute = 420))
    }

    @Test
    fun midnightCrossingWindow_earlyMorningSide() {
        // 22:00 (1320) - 07:00 (420): 03:00 (180) should match.
        assertTrue(RulePrecedenceResolver.isWithinWindow(minute = 180, startMinute = 1320, endMinute = 420))
    }

    @Test
    fun midnightCrossingWindow_outsideWindow() {
        // 22:00 (1320) - 07:00 (420): noon (720) should not match.
        assertFalse(RulePrecedenceResolver.isWithinWindow(minute = 720, startMinute = 1320, endMinute = 420))
    }

    @Test
    fun midnightCrossingWindow_atStart_isInclusive() {
        assertTrue(RulePrecedenceResolver.isWithinWindow(minute = 1320, startMinute = 1320, endMinute = 420))
    }

    @Test
    fun midnightCrossingWindow_atEnd_isExclusive() {
        assertFalse(RulePrecedenceResolver.isWithinWindow(minute = 420, startMinute = 1320, endMinute = 420))
    }

    @Test
    fun emptyWindow_startEqualsEnd_neverMatches() {
        assertFalse(RulePrecedenceResolver.isWithinWindow(minute = 600, startMinute = 600, endMinute = 600))
    }

    // --- evaluate(): end-to-end through the resolver ---

    @Test
    fun scheduleActive_blocksUnmatchedNumber() =
        runTest {
            val rule = ScheduleRule(id = 1, label = "Nighttime", startMinute = 1320, endMinute = 420, enabled = true)
            val ctx =
                emptyContext.copy(
                    enabledScheduleRules = listOf(rule),
                    currentLocalMinuteOfDay = 1410,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.ScheduleBlock)
            assertTrue(decision.isBlocked)
            assertEquals("SCHEDULE", decision.ruleTypeTag)
            assertEquals("Nighttime", decision.blockReason)
            assertEquals(1L, decision.loggedRuleId)
        }

    @Test
    fun scheduleInactive_outsideWindow_allowsThrough() =
        runTest {
            val rule = ScheduleRule(id = 1, label = "Nighttime", startMinute = 1320, endMinute = 420, enabled = true)
            val ctx =
                emptyContext.copy(
                    enabledScheduleRules = listOf(rule),
                    currentLocalMinuteOfDay = 720,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.DefaultAllow)
        }

    @Test
    fun allowlistOverridesActiveSchedule() =
        runTest {
            val rule = ScheduleRule(id = 1, label = "Nighttime", startMinute = 1320, endMinute = 420, enabled = true)
            val ctx =
                emptyContext.copy(
                    allowlistedNumbers = setOf("+34600123456"),
                    enabledScheduleRules = listOf(rule),
                    currentLocalMinuteOfDay = 1410,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.Allowlist)
            assertFalse(decision.isBlocked)
        }

    @Test
    fun manualBlockTakesPrecedenceOverSchedule() =
        runTest {
            val rule = ScheduleRule(id = 1, label = "Nighttime", startMinute = 1320, endMinute = 420, enabled = true)
            val ctx =
                emptyContext.copy(
                    blockedNumbers = setOf("+34600123456"),
                    enabledScheduleRules = listOf(rule),
                    currentLocalMinuteOfDay = 1410,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.ManualBlock)
        }

    @Test
    fun noCurrentMinuteProvided_skipsScheduleCheck() =
        runTest {
            val rule = ScheduleRule(id = 1, label = "Nighttime", startMinute = 1320, endMinute = 420, enabled = true)
            val ctx =
                emptyContext.copy(
                    enabledScheduleRules = listOf(rule),
                    currentLocalMinuteOfDay = null,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.DefaultAllow)
        }

    @Test
    fun multipleScheduleRules_matchesEitherWindow() =
        runTest {
            val morning = ScheduleRule(id = 1, label = "Morning meeting", startMinute = 540, endMinute = 600, enabled = true)
            val night = ScheduleRule(id = 2, label = "Nighttime", startMinute = 1320, endMinute = 420, enabled = true)
            val ctx =
                emptyContext.copy(
                    enabledScheduleRules = listOf(morning, night),
                    currentLocalMinuteOfDay = 570,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.ScheduleBlock)
            assertEquals(1L, decision.loggedRuleId)
        }
}
