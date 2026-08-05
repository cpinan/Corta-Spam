package org.carlospinan.bloqueador.app.rules

/**
 * Represents the outcome of evaluating an incoming call against all rules.
 * Precedence order: MANUAL_BLOCK > ALLOWLIST > PATTERN > COUNTRY > SPAM > ACTION > SCHEDULE > DEFAULT_ALLOW.
 */
sealed class RuleDecision {
    /**
     * Number is on the contacts or manual allowlist — bypass all block rules.
     * [ruleId]/[label] are only populated for an explicit allowlist-entry match (-1/null when
     * the match came from a contact instead, since contacts don't have a rule row).
     */
    data class Allowlist(
        val ruleId: Long,
        val label: String?,
    ) : RuleDecision()

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

    /** Number called during an enabled quiet-hours window and isn't allowlisted. */
    data class ScheduleBlock(
        val ruleId: Long,
        val label: String?,
    ) : RuleDecision()

    /** No blocking rule matched — call is allowed through (settings default action ALLOW). */
    data object DefaultAllow : RuleDecision()

    /** No blocking rule matched, but settings default action is BLOCK. */
    data object DefaultBlock : RuleDecision()

    /**
     * No blocking rule matched, settings default action is ASK. The call can't be interrupted
     * mid-ring to literally ask the user, so it's let through like [DefaultAllow] but tagged
     * distinctly in the call log as a "needs review" entry the user can revisit later.
     */
    data object PendingReview : RuleDecision()

    /**
     * No rule matched and default action is BLOCK, but the number has retried at least
     * [attempts] times within the retention window — let it through instead, per the
     * "repeated caller bypass" setting. Only reachable from the default-block path; a manual
     * block/pattern/country/spam/action/schedule match always takes precedence over this.
     */
    data class AllowedAfterRepeatedAttempts(
        val attempts: Int,
    ) : RuleDecision()

    /** Whether this decision results in the call being blocked. */
    val isBlocked: Boolean
        get() =
            this !is Allowlist &&
                this !is DefaultAllow &&
                this !is PendingReview &&
                this !is AllowedAfterRepeatedAttempts

    /**
     * Why this decision was reached, or null when there's nothing worth saying.
     *
     * Structured, not a sentence: the app ships in four locales, and this value is both shown
     * in notifications and written into `CallLogEntry.rule_detail`. Building the English words
     * here meant every user saw English, and every historical log row stayed English forever.
     * See [BlockReason]. A rule the user gave a label to reports that label verbatim.
     */
    val reason: BlockReason?
        get() =
            when (this) {
                is Allowlist -> label?.let { BlockReason.Custom(it) }
                is ManualBlock -> label?.let { BlockReason.Custom(it) } ?: BlockReason.ManuallyBlocked
                is PatternBlock -> label?.let { BlockReason.Custom(it) } ?: BlockReason.PatternMatch(pattern)
                is CountryBlock -> BlockReason.Country(countryCode = countryCode, countryName = countryName)
                is SpamHit -> BlockReason.Spam(source = source, confidencePercent = (confidence * 100).toInt())
                is ActionBlock ->
                    label?.let { BlockReason.Custom(it) }
                        ?: BlockReason.RepeatedCalls(attempts = attempts, windowMinutes = windowMinutes)
                is ScheduleBlock -> label?.let { BlockReason.Custom(it) } ?: BlockReason.QuietHours
                is DefaultAllow -> null
                is DefaultBlock -> BlockReason.NoMatchingRule
                is PendingReview -> null
                is AllowedAfterRepeatedAttempts -> BlockReason.AllowedAfterRepeatedAttempts(attempts)
            }

    /** [reason] in the form the call log persists, or null when there's nothing to record. */
    val loggedDetail: String?
        get() = reason?.let { BlockReasonCodec.encode(it) }

    /** Id of the rule that fired, for persistence in the call log, or null when not applicable. */
    val loggedRuleId: Long?
        get() =
            when (this) {
                is Allowlist -> ruleId.takeIf { it != -1L }
                is ManualBlock -> ruleId
                is PatternBlock -> ruleId
                is CountryBlock -> ruleId
                is ActionBlock -> ruleId
                is ScheduleBlock -> ruleId
                is SpamHit, is DefaultAllow, is DefaultBlock, is PendingReview, is AllowedAfterRepeatedAttempts -> null
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
                is ScheduleBlock -> "SCHEDULE"
                is DefaultAllow -> null
                is DefaultBlock -> null
                is PendingReview -> "REVIEW"
                is AllowedAfterRepeatedAttempts -> "REPEATED_ALLOWED"
            }
}
