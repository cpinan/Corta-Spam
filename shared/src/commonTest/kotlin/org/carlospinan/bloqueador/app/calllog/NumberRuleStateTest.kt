package org.carlospinan.bloqueador.app.calllog

import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumberRuleStateTest {
    private fun blocked(
        id: Long,
        number: String,
    ) = BlockedNumberEntry(id = id, number = number, label = null, createdAt = 0)

    private fun allowed(
        id: Long,
        number: String,
    ) = AllowlistedNumberEntry(id = id, number = number, label = null, createdAt = 0)

    @Test
    fun aNumberWithNoRuleHasNoState() {
        val state = numberRuleState("+34611998877", emptyList(), emptyList())

        assertFalse(state.isBlocked)
        assertFalse(state.isAllowlisted)
        assertEquals(NumberRuleState.None, state)
    }

    @Test
    fun aBlockedNumberCarriesTheRuleIdToUndoIt() {
        val state = numberRuleState("+34611998877", listOf(blocked(7, "+34611998877")), emptyList())

        assertTrue(state.isBlocked)
        assertEquals(7, state.blockedRuleId)
    }

    @Test
    fun anAllowlistedNumberCarriesItsOwnRuleId() {
        val state = numberRuleState("+34611998877", emptyList(), listOf(allowed(9, "+34611998877")))

        assertTrue(state.isAllowlisted)
        assertEquals(9, state.allowlistedRuleId)
    }

    /**
     * The rule may have been typed nationally while the call arrived in E.164. Comparing the
     * strings would offer "Block this number" for a caller who is already on the list — a tap
     * that does nothing the user can see.
     */
    @Test
    fun aRuleSavedNationallyMatchesTheSameCallerInE164() {
        val state = numberRuleState("+34611998877", listOf(blocked(1, "611 99 88 77")), emptyList())

        assertTrue(state.isBlocked)
    }

    @Test
    fun aRuleSavedInE164MatchesTheSameCallerInNationalForm() {
        val state = numberRuleState("611998877", listOf(blocked(1, "+34611998877")), emptyList())

        assertTrue(state.isBlocked)
    }

    /** Two international numbers sharing a national form are different subscribers. */
    @Test
    fun aRuleForAnotherCountryDoesNotClaimTheCaller() {
        val state = numberRuleState("+34611998877", listOf(blocked(1, "+51611998877")), emptyList())

        assertFalse(state.isBlocked)
    }

    /** A withheld caller has no number to act on, so neither action should be offered as an undo. */
    @Test
    fun aBlankNumberHasNoRuleState() {
        val state = numberRuleState("", listOf(blocked(1, "+34611998877")), listOf(allowed(2, "+34611998877")))

        assertEquals(NumberRuleState.None, state)
    }

    @Test
    fun bothListsCanClaimTheSameNumber() {
        val state =
            numberRuleState(
                "+34611998877",
                listOf(blocked(1, "+34611998877")),
                listOf(allowed(2, "+34611998877")),
            )

        assertTrue(state.isBlocked)
        assertTrue(state.isAllowlisted)
    }

    @Test
    fun statesAreBuiltForEveryDistinctNumberAndOmittedWhenThereIsNoRule() {
        val states =
            numberRuleStates(
                listOf("+34611998877", "+34611998877", "+34600000000"),
                listOf(blocked(1, "+34611998877")),
                emptyList(),
            )

        assertEquals(setOf("+34611998877"), states.keys)
        assertEquals(1, states.getValue("+34611998877").blockedRuleId)
    }
}
