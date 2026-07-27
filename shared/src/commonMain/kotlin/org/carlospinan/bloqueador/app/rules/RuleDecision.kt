package org.carlospinan.bloqueador.app.rules

/**
 * Represents the outcome of evaluating an incoming call against all rules.
 * Precedence order: ALLOWLIST > MANUAL_BLOCK > PATTERN > COUNTRY > SPAM > ACTION > DEFAULT_ALLOW.
 */
sealed class RuleDecision {
    /** Number is on the contacts or manual allowlist — bypass all block rules. */
    data object Allowlist : RuleDecision()

    /** Number matches a manual block entry. */
    data class ManualBlock(
        val ruleId: Long,
        val label: String?,
    ) : RuleDecision()

    /** Number matches an enabled pattern rule. */
    data class PatternBlock(
        val ruleId: Long,
        val pattern: String,
        val label: String?,
    ) : RuleDecision()

    /** Number's parsed country code matches an enabled country rule. */
    data class CountryBlock(
        val ruleId: Long,
        val countryCode: String,
        val countryName: String,
    ) : RuleDecision()

    /** Number flagged by an external spam provider. */
    data class SpamHit(
        val confidence: Float,
        val source: String,
    ) : RuleDecision()

    /** Number crossed an action-rule attempt threshold within its window. */
    data class ActionBlock(
        val ruleId: Long,
        val label: String?,
        val attempts: Int,
        val windowMinutes: Int,
    ) : RuleDecision()

    /** No blocking rule matched — call is allowed through (settings default action ALLOW/ASK). */
    data object DefaultAllow : RuleDecision()

    /** No blocking rule matched, but settings default action is BLOCK. */
    data object DefaultBlock : RuleDecision()

    /** Whether this decision results in the call being blocked. */
    val isBlocked: Boolean
        get() = this !is Allowlist && this !is DefaultAllow

    /** Human-readable reason for the decision, or null if allowed. */
    val blockReason: String?
        get() =
            when (this) {
                is Allowlist -> null
                is ManualBlock -> label ?: "Manually blocked"
                is PatternBlock -> label ?: "Pattern match: $pattern"
                is CountryBlock -> "Country: $countryName ($countryCode)"
                is SpamHit -> "Spam ($source, ${(confidence * 100).toInt()}%)"
                is ActionBlock -> label ?: "Repeated calls ($attempts in ${windowMinutes}m)"
                is DefaultAllow -> null
                is DefaultBlock -> "No matching rule (default: block)"
            }

    /** Id of the rule that fired, for persistence in the call log, or null when not applicable. */
    val loggedRuleId: Long?
        get() =
            when (this) {
                is ManualBlock -> ruleId
                is PatternBlock -> ruleId
                is CountryBlock -> ruleId
                is ActionBlock -> ruleId
                is Allowlist, is SpamHit, is DefaultAllow, is DefaultBlock -> null
            }

    /** Rule type tag for persistence in the call log. */
    val ruleTypeTag: String?
        get() =
            when (this) {
                is Allowlist -> "CONTACTS"
                is ManualBlock -> "MANUAL"
                is PatternBlock -> "PATTERN"
                is CountryBlock -> "COUNTRY"
                is SpamHit -> "SPAM"
                is ActionBlock -> "ACTION"
                is DefaultAllow -> null
                is DefaultBlock -> null
            }
}
