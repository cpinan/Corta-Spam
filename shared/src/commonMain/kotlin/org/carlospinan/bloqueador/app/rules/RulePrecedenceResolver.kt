package org.carlospinan.bloqueador.app.rules

import org.carlospinan.bloqueador.app.settings.DefaultAction
import org.carlospinan.bloqueador.app.spam.SpamProviderClient

/**
 * Evaluates an incoming phone number against the rule set in precedence order:
 * 1. Manual block — exact number match (overrides allowlist and contacts)
 * 2. Contacts/allowlist — always bypasses blocks
 * 3. Pattern — glob/contains match against enabled patterns
 * 4. Country — country code match against enabled country rules
 * 5. Spam — external provider lookup (if enabled)
 * 6. Action — repeated-call threshold within time window
 * 7. Schedule — quiet-hours window active
 * 8. Default — allow through
 *
 * This is stateless and pure — all data is passed in, making it trivially testable.
 */
data class ResolveContext(
    val allowlistedNumbers: Set<String>,
    val contactNumbers: Set<String> = emptySet(),
    val blockedNumbers: Set<String>,
    /** Full entries for numbers in [blockedNumbers], keyed by number — supplies real id/label when known. */
    val blockedNumberDetails: Map<String, BlockedNumberEntry> = emptyMap(),
    val enabledPatterns: List<PatternRule>,
    val enabledCountryCodes: Set<String>,
    /** Full entries for codes in [enabledCountryCodes], keyed by code — supplies real id/name when known. */
    val countryRuleDetails: Map<String, CountryRuleEntry> = emptyMap(),
    val spamProvider: SpamProviderClient? = null,
    val spamEnabled: Boolean = false,
    val enabledActionRules: List<ActionRule> = emptyList(),
    /** Pre-computed attempt counts keyed by window minutes (for each distinct window). */
    val attemptCountsByWindowMinutes: Map<Int, Int> = emptyMap(),
    val enabledScheduleRules: List<ScheduleRule> = emptyList(),
    /** Minutes since local midnight (0-1439) at evaluation time, or null to skip schedule checks. */
    val currentLocalMinuteOfDay: Int? = null,
    /** What to do when no rule matches — settings-controlled (ALLOW/BLOCK/ASK). */
    val defaultAction: DefaultAction = DefaultAction.ALLOW,
)

object RulePrecedenceResolver {
    suspend fun evaluate(
        number: String,
        context: ResolveContext,
        parseCountryCode: (String) -> String? = { PhoneNumberParser.parseCountryCode(it) },
    ): RuleDecision {
        val normalized = PhoneNumberParser.normalizeForComparison(number)
        if (normalized.isEmpty()) {
            // No PendingReview here even when defaultAction is ASK: there's no number to
            // resurface in a review queue for a blank/private caller.
            return context.defaultAction.let {
                if (it ==
                    DefaultAction.BLOCK
                ) {
                    RuleDecision.DefaultBlock
                } else {
                    RuleDecision.DefaultAllow
                }
            }
        }

        val normalizedAllowlist = context.allowlistedNumbers.map { PhoneNumberParser.normalizeForComparison(it) }.toSet()
        val normalizedContacts = context.contactNumbers.map { PhoneNumberParser.normalizeForComparison(it) }.toSet()
        val normalizedBlocked = context.blockedNumbers.map { PhoneNumberParser.normalizeForComparison(it) }.toSet()

        val mergedAllowlist = normalizedAllowlist + normalizedContacts

        // 1. Manual block check (highest priority — overrides allowlist and contacts)
        if (normalized in normalizedBlocked) {
            val entry =
                context.blockedNumberDetails.entries
                    .firstOrNull {
                        PhoneNumberParser.normalizeForComparison(it.key) == normalized
                    }?.value
            return RuleDecision.ManualBlock(ruleId = entry?.id ?: -1, label = entry?.label)
        }

        // 2. Allowlist check (contacts + manual allowlist)
        if (normalized in mergedAllowlist) {
            return RuleDecision.Allowlist
        }

        // 3. Pattern match
        for (pattern in context.enabledPatterns) {
            if (matchesPattern(normalized, pattern.pattern)) {
                return RuleDecision.PatternBlock(
                    ruleId = pattern.id,
                    pattern = pattern.pattern,
                    label = pattern.label,
                )
            }
        }

        // 4. Country code match
        val countryCode = parseCountryCode(normalized)
        if (countryCode != null && countryCode in context.enabledCountryCodes) {
            val entry = context.countryRuleDetails[countryCode]
            return RuleDecision.CountryBlock(
                ruleId = entry?.id ?: -1,
                countryCode = countryCode,
                countryName = entry?.countryName ?: countryCode,
            )
        }

        // 5. Spam provider (if enabled)
        if (context.spamEnabled && context.spamProvider != null) {
            val result = context.spamProvider.lookup(normalized)
            if (result != null && result.isSpam) {
                return RuleDecision.SpamHit(
                    confidence = result.confidence,
                    source = result.source,
                )
            }
        }

        // 6. Action rules (block after N attempts within window)
        for (rule in context.enabledActionRules) {
            if (rule.patternId != null) {
                val pattern = context.enabledPatterns.find { it.id == rule.patternId }
                if (pattern == null || !matchesPattern(normalized, pattern.pattern)) continue
            }
            val count = context.attemptCountsByWindowMinutes[rule.windowMinutes] ?: 0
            if (count >= rule.attempts) {
                return RuleDecision.ActionBlock(
                    ruleId = rule.id,
                    label = rule.label,
                    attempts = rule.attempts,
                    windowMinutes = rule.windowMinutes,
                )
            }
        }

        // 7. Schedule rules (quiet hours)
        val currentMinute = context.currentLocalMinuteOfDay
        if (currentMinute != null) {
            for (rule in context.enabledScheduleRules) {
                if (isWithinWindow(currentMinute, rule.startMinute, rule.endMinute)) {
                    return RuleDecision.ScheduleBlock(ruleId = rule.id, label = rule.label)
                }
            }
        }

        // 8. Default: honor the user's chosen default action.
        return when (context.defaultAction) {
            DefaultAction.BLOCK -> RuleDecision.DefaultBlock
            DefaultAction.ASK -> RuleDecision.PendingReview
            DefaultAction.ALLOW -> RuleDecision.DefaultAllow
        }
    }

    /**
     * True when [minute] (0-1439) falls in the half-open window [startMinute, endMinute).
     * When endMinute <= startMinute the window crosses midnight, e.g. 22:00-07:00.
     */
    internal fun isWithinWindow(
        minute: Int,
        startMinute: Int,
        endMinute: Int,
    ): Boolean =
        if (startMinute <= endMinute) {
            minute in startMinute until endMinute
        } else {
            minute >= startMinute || minute < endMinute
        }

    /**
     * Simple glob-style pattern matching.
     * Supports:
     * - Prefix: "+34900*" matches "+34900123456"
     * - Suffix: "*1234" matches "+341234"
     * - Contains: "*900*" matches "+34900123456"
     * - Exact: "+34900" matches only "+34900"
     */
    internal fun matchesPattern(
        number: String,
        pattern: String,
    ): Boolean {
        val trimmed = pattern.trim()
        if (trimmed.isEmpty()) return false

        val startsWithStar = trimmed.startsWith('*')
        val endsWithStar = trimmed.endsWith('*')
        val core = trimmed.trim('*')
        val normalizedCore = PhoneNumberParser.normalizeForComparison(core)
        val normalizedNumber = PhoneNumberParser.normalizeForComparison(number)

        return when {
            startsWithStar && endsWithStar -> normalizedNumber.contains(normalizedCore)
            startsWithStar -> normalizedNumber.endsWith(normalizedCore)
            endsWithStar -> normalizedNumber.startsWith(normalizedCore)
            else -> normalizedNumber == normalizedCore
        }
    }
}
