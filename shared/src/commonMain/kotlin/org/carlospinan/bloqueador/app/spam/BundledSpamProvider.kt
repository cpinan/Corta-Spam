package org.carlospinan.bloqueador.app.spam

/**
 * Bundled local spam heuristics — a small on-device list of known spam prefixes
 * and patterns shipped with the app. Checked before the external [NoOpSpamProvider]
 * (or future pluggable providers). No network — all local.
 *
 * SPAM_PREFIXES covers known high-spam country/area codes and common scam prefixes.
 * SPAM_PATTERNS are glob-style patterns checked via the same [matchesPattern] logic
 * as the resolver uses.
 */
class BundledSpamProvider : SpamProviderClient {
    override suspend fun lookup(number: String): SpamResult? {
        val cleaned = number.trim()
        for (prefix in SPAM_PREFIXES) {
            if (cleaned.startsWith(prefix)) {
                return SpamResult(isSpam = true, confidence = 0.7f, source = "bundled")
            }
        }
        for (pattern in SPAM_PATTERNS) {
            if (matchesPattern(cleaned, pattern)) {
                return SpamResult(isSpam = true, confidence = 0.65f, source = "bundled")
            }
        }
        return null
    }

    companion object {
        private val SPAM_PREFIXES = setOf(
            "+225",    // Côte d'Ivoire — high-volume robocall origin
            "+234",    // Nigeria — known scam call hub
            "+237",    // Cameroon
            "+242",    // Congo
            "+249",    // Sudan
            "+256",    // Uganda
            "+257",    // Burundi
            "+263",    // Zimbabwe
            "+380",    // Ukraine — one-ring scams
            "+381",    // Serbia
            "+387",    // Bosnia
            "+389",    // North Macedonia
            "+880",    // Bangladesh
            "+92",     // Pakistan
            "+212",    // Morocco
            "+218",    // Libya
            "+224",    // Guinea
            "+231",    // Liberia
            "+232",    // Sierra Leone
            "+244",    // Angola
            "+252",    // Somalia
            "+261",    // Madagascar
            "+267",    // Botswana
            "+269",    // Comoros
            "+27",     // South Africa — high wangiri volume
            "+355",    // Albania
            "+370",    // Lithuania
            "+371",    // Latvia
            "+372",    // Estonia
            "+373",    // Moldova
            "+375",    // Belarus
            "+7",      // Russia/Kazakhstan
        )

        private val SPAM_PATTERNS = setOf(
            "+*000*",  // Toll-free service calls pretending to be real numbers
        )
    }
}

private fun matchesPattern(
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
