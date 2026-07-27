package org.carlospinan.bloqueador.app.rules

import org.carlospinan.bloqueador.app.spam.SpamProviderClient

/**
 * Evaluates an incoming phone number against the rule set in precedence order:
 * 1. Contacts/allowlist — always bypasses blocks
 * 2. Manual block — exact number match
 * 3. Pattern — glob/contains match against enabled patterns
 * 4. Country — country code match against enabled country rules
 * 5. Spam — external provider lookup (if enabled)
 * 6. Action — repeated-call threshold within time window
 * 7. Default — allow through
 *
 * This is stateless and pure — all data is passed in, making it trivially testable.
 */
data class ResolveContext(
    val allowlistedNumbers: Set<String>,
    val contactNumbers: Set<String> = emptySet(),
    val blockedNumbers: Set<String>,
    val enabledPatterns: List<PatternRule>,
    val enabledCountryCodes: Set<String>,
    val spamProvider: SpamProviderClient? = null,
    val spamEnabled: Boolean = false,
    val enabledActionRules: List<ActionRule> = emptyList(),
    /** Pre-computed attempt counts keyed by window minutes (for each distinct window). */
    val attemptCountsByWindowMinutes: Map<Int, Int> = emptyMap(),
)

object RulePrecedenceResolver {
    suspend fun evaluate(
        number: String,
        context: ResolveContext,
        parseCountryCode: (String) -> String? = { PhoneNumberParser.parseCountryCode(it) },
    ): RuleDecision {
        val normalized = number.trim()

        val mergedAllowlist = context.allowlistedNumbers + context.contactNumbers

        // 1. Allowlist check (highest priority — contacts + manual allowlist)
        if (normalized in mergedAllowlist) {
            return RuleDecision.Allowlist
        }

        // 2. Manual block check
        if (normalized in context.blockedNumbers) {
            return RuleDecision.ManualBlock(ruleId = -1, label = null)
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
            return RuleDecision.CountryBlock(
                ruleId = -1,
                countryCode = countryCode,
                countryName = countryCode,
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

        // 7. Default: allow
        return RuleDecision.DefaultAllow
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

        return when {
            startsWithStar && endsWithStar -> number.contains(core, ignoreCase = true)
            startsWithStar -> number.endsWith(core, ignoreCase = true)
            endsWithStar -> number.startsWith(core, ignoreCase = true)
            else -> number.equals(core, ignoreCase = true)
        }
    }
}
