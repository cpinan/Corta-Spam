package org.carlospinan.bloqueador.app.rules

/**
 * Evaluates an incoming phone number against the rule set in precedence order:
 * 1. Contacts/allowlist — always bypasses blocks
 * 2. Manual block — exact number match
 * 3. Pattern — glob/contains match against enabled patterns
 * 4. Country — country code match against enabled country rules
 * 5. Default — allow through
 *
 * This is stateless and pure — all data is passed in, making it trivially testable.
 */
data class ResolveContext(
    val allowlistedNumbers: Set<String>,
    val contactNumbers: Set<String> = emptySet(),
    val blockedNumbers: Set<String>,
    val enabledPatterns: List<PatternRule>,
    val enabledCountryCodes: Set<String>,
)

object RulePrecedenceResolver {
    fun evaluate(
        number: String,
        context: ResolveContext,
        parseCountryCode: (String) -> String? = { PhoneNumberParser.parseCountryCode(it) },
    ): RuleDecision =
        evaluate(
            number = number,
            allowlistedNumbers = context.allowlistedNumbers,
            contactNumbers = context.contactNumbers,
            blockedNumbers = context.blockedNumbers,
            enabledPatterns = context.enabledPatterns,
            enabledCountryCodes = context.enabledCountryCodes,
            parseCountryCode = parseCountryCode,
        )

    fun evaluate(
        number: String,
        allowlistedNumbers: Set<String>,
        contactNumbers: Set<String> = emptySet(),
        blockedNumbers: Set<String>,
        enabledPatterns: List<PatternRule>,
        enabledCountryCodes: Set<String>,
        parseCountryCode: (String) -> String? = { PhoneNumberParser.parseCountryCode(it) },
    ): RuleDecision {
        val normalized = number.trim()

        val mergedAllowlist = allowlistedNumbers + contactNumbers

        // 1. Allowlist check (highest priority — contacts + manual allowlist)
        if (normalized in mergedAllowlist) {
            return RuleDecision.Allowlist
        }

        // 2. Manual block check
        if (normalized in blockedNumbers) {
            return RuleDecision.ManualBlock(ruleId = -1, label = null)
        }

        // 3. Pattern match
        for (pattern in enabledPatterns) {
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
        if (countryCode != null && countryCode in enabledCountryCodes) {
            return RuleDecision.CountryBlock(
                ruleId = -1,
                countryCode = countryCode,
                countryName = countryCode,
            )
        }

        // 5. Default: allow
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
