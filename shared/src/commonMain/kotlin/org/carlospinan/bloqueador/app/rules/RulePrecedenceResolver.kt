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
    val blockedNumbers: Set<String>,
    val enabledPatterns: List<PatternRule>,
    val enabledCountryCodes: Set<String>,
)

object RulePrecedenceResolver {
    fun evaluate(
        number: String,
        context: ResolveContext,
        parseCountryCode: (String) -> String? = { libphonenumberParseCountryCode(it) },
    ): RuleDecision =
        evaluate(
            number = number,
            allowlistedNumbers = context.allowlistedNumbers,
            blockedNumbers = context.blockedNumbers,
            enabledPatterns = context.enabledPatterns,
            enabledCountryCodes = context.enabledCountryCodes,
            parseCountryCode = parseCountryCode,
        )

    fun evaluate(
        number: String,
        allowlistedNumbers: Set<String>,
        blockedNumbers: Set<String>,
        enabledPatterns: List<PatternRule>,
        enabledCountryCodes: Set<String>,
        parseCountryCode: (String) -> String? = { libphonenumberParseCountryCode(it) },
    ): RuleDecision {
        val normalized = number.trim()

        // 1. Allowlist check (highest priority)
        if (normalized in allowlistedNumbers) {
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

    /**
     * Minimal country code parser. Extracts the E.164 country code from a
     * phone number string. This is a placeholder — M4 will integrate
     * libphonenumber for full parsing.
     */
    internal fun libphonenumberParseCountryCode(number: String): String? {
        val cleaned = number.trim().removePrefix("+")
        if (cleaned.isEmpty() || !cleaned.all { it.isDigit() }) return null

        // Common country code prefixes (E.164)
        return when {
            cleaned.startsWith("1") && cleaned.length >= 11 -> "1" // US/CA
            cleaned.startsWith("44") && cleaned.length >= 12 -> "44" // UK
            cleaned.startsWith("34") && cleaned.length >= 11 -> "34" // Spain
            cleaned.startsWith("33") && cleaned.length >= 11 -> "33" // France
            cleaned.startsWith("49") && cleaned.length >= 12 -> "49" // Germany
            cleaned.startsWith("39") && cleaned.length >= 11 -> "39" // Italy
            cleaned.startsWith("52") && cleaned.length >= 12 -> "52" // Mexico
            cleaned.startsWith("55") && cleaned.length >= 12 -> "55" // Brazil
            cleaned.startsWith("81") && cleaned.length >= 12 -> "81" // Japan
            cleaned.startsWith("86") && cleaned.length >= 12 -> "86" // China
            cleaned.startsWith("91") && cleaned.length >= 12 -> "91" // India
            cleaned.startsWith("61") && cleaned.length >= 11 -> "61" // Australia
            cleaned.startsWith("7") && cleaned.length >= 11 -> "7" // Russia
            cleaned.startsWith("27") && cleaned.length >= 11 -> "27" // South Africa
            cleaned.startsWith("20") && cleaned.length >= 11 -> "20" // Egypt
            cleaned.startsWith("90") && cleaned.length >= 11 -> "90" // Turkey
            cleaned.startsWith("48") && cleaned.length >= 11 -> "48" // Poland
            cleaned.startsWith("31") && cleaned.length >= 11 -> "31" // Netherlands
            cleaned.startsWith("46") && cleaned.length >= 11 -> "46" // Sweden
            cleaned.startsWith("47") && cleaned.length >= 11 -> "47" // Norway
            cleaned.startsWith("45") && cleaned.length >= 11 -> "45" // Denmark
            cleaned.startsWith("358") && cleaned.length >= 12 -> "358" // Finland
            cleaned.startsWith("41") && cleaned.length >= 11 -> "41" // Switzerland
            cleaned.startsWith("43") && cleaned.length >= 11 -> "43" // Austria
            cleaned.startsWith("32") && cleaned.length >= 11 -> "32" // Belgium
            cleaned.startsWith("351") && cleaned.length >= 12 -> "351" // Portugal
            cleaned.startsWith("30") && cleaned.length >= 11 -> "30" // Greece
            cleaned.startsWith("972") && cleaned.length >= 12 -> "972" // Israel
            cleaned.startsWith("971") && cleaned.length >= 12 -> "971" // UAE
            cleaned.startsWith("966") && cleaned.length >= 12 -> "966" // Saudi Arabia
            cleaned.startsWith("63") && cleaned.length >= 11 -> "63" // Philippines
            cleaned.startsWith("62") && cleaned.length >= 12 -> "62" // Indonesia
            cleaned.startsWith("66") && cleaned.length >= 11 -> "66" // Thailand
            cleaned.startsWith("84") && cleaned.length >= 11 -> "84" // Vietnam
            cleaned.startsWith("60") && cleaned.length >= 11 -> "60" // Malaysia
            cleaned.startsWith("65") && cleaned.length >= 11 -> "65" // Singapore
            cleaned.startsWith("82") && cleaned.length >= 12 -> "82" // South Korea
            cleaned.startsWith("54") && cleaned.length >= 12 -> "54" // Argentina
            cleaned.startsWith("56") && cleaned.length >= 11 -> "56" // Chile
            cleaned.startsWith("57") && cleaned.length >= 12 -> "57" // Colombia
            cleaned.startsWith("51") && cleaned.length >= 11 -> "51" // Peru
            else -> null
        }
    }
}
