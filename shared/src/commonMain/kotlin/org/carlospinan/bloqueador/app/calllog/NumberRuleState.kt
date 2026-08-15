package org.carlospinan.bloqueador.app.calllog

import org.carlospinan.bloqueador.app.rules.AllowlistedNumberEntry
import org.carlospinan.bloqueador.app.rules.BlockedNumberEntry
import org.carlospinan.bloqueador.app.rules.PhoneNumberParser

/**
 * Whether the user's own rules currently block or allow a number, and the row ids needed to undo
 * either — the call log's Block/Unblock action has to remove a specific rule, not a number.
 *
 * This is **not** the same question as "was this call blocked", which is what
 * [org.carlospinan.bloqueador.app.rules.CallLogEntryData.action] records. That is a fact about
 * one call at the moment it arrived; this is a fact about the rule set right now. They disagree
 * constantly and both are worth showing: a call blocked last week by a number the user has since
 * unblocked, or a call that rang through before the user blocked the caller. The log offered
 * "Block this number" for both, so blocking an already-blocked caller looked like a no-op the
 * user could not tell from a bug.
 */
data class NumberRuleState(
    /** Id of the manual block rule matching this number, or null when nothing blocks it. */
    val blockedRuleId: Long? = null,
    /** Id of the allowlist rule matching this number, or null when it isn't allowlisted. */
    val allowlistedRuleId: Long? = null,
) {
    val isBlocked: Boolean get() = blockedRuleId != null
    val isAllowlisted: Boolean get() = allowlistedRuleId != null

    companion object {
        val None = NumberRuleState()
    }
}

/**
 * The rule state of [number], matched with [PhoneNumberParser.sameNumber] rather than string
 * equality: the rule may have been saved nationally while the call arrived in E.164, and a
 * screen that compared the strings would offer "Block" for a caller who is already blocked.
 *
 * A blank number (a withheld caller) has no rule state — there is nothing to block.
 */
fun numberRuleState(
    number: String,
    blockedNumbers: List<BlockedNumberEntry>,
    allowlistedNumbers: List<AllowlistedNumberEntry>,
): NumberRuleState {
    if (number.isBlank()) return NumberRuleState.None
    return NumberRuleState(
        blockedRuleId = blockedNumbers.firstOrNull { PhoneNumberParser.sameNumber(it.number, number) }?.id,
        allowlistedRuleId = allowlistedNumbers.firstOrNull { PhoneNumberParser.sameNumber(it.number, number) }?.id,
    )
}

/**
 * Rule state for every number in [numbers], as one pass over the rule lists.
 *
 * The call log renders a badge per row, and doing the lookup inside the row composable turned a
 * screenful of calls into rows × rules `sameNumber` comparisons on every recomposition — while
 * the user types in the search box. Keyed by the number string as the log stores it, so a row
 * looks its own state up by identity.
 */
fun numberRuleStates(
    numbers: Collection<String>,
    blockedNumbers: List<BlockedNumberEntry>,
    allowlistedNumbers: List<AllowlistedNumberEntry>,
): Map<String, NumberRuleState> =
    numbers
        .distinct()
        .associateWith { numberRuleState(it, blockedNumbers, allowlistedNumbers) }
        .filterValues { it != NumberRuleState.None }
