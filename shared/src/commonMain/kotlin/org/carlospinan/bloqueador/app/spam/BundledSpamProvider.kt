package org.carlospinan.bloqueador.app.spam

/**
 * Bundled local spam heuristics — a small on-device list of known high-volume scam-origin
 * country codes shipped with the app. The only [SpamProviderClient] implementation bound
 * today. No network — all local.
 *
 * Entries are stored in `+E.164` form and matched with `startsWith`, which is exactly the form
 * [SpamProviderClient.lookup] promises for an international handle. A national-format handle
 * reaches here as-is and matches nothing, which is correct: it carries no country to match.
 *
 * There is deliberately no shape-based pattern list. The one that used to be here (`"+*000*"`,
 * meant to catch numbers padded with zeros) was unreachable dead code, and re-implementing it
 * live would have blocked real subscribers whose numbers happen to contain a run of zeros —
 * a heuristic that has never been measured against real traffic doesn't belong on a path that
 * silently rejects calls.
 */
class BundledSpamProvider : SpamProviderClient {
    override suspend fun lookup(number: String): SpamResult? {
        val cleaned = number.trim()
        for (prefix in SPAM_PREFIXES) {
            if (cleaned.startsWith(prefix)) {
                return SpamResult(isSpam = true, confidence = 0.7f, source = "bundled")
            }
        }
        return null
    }

    companion object {
        private val SPAM_PREFIXES =
            setOf(
                "+225", // Côte d'Ivoire — high-volume robocall origin
                "+234", // Nigeria — known scam call hub
                "+237", // Cameroon
                "+242", // Congo
                "+249", // Sudan
                "+256", // Uganda
                "+257", // Burundi
                "+263", // Zimbabwe
                "+380", // Ukraine — one-ring scams
                "+381", // Serbia
                "+387", // Bosnia
                "+389", // North Macedonia
                "+880", // Bangladesh
                "+92", // Pakistan
                "+212", // Morocco
                "+218", // Libya
                "+224", // Guinea
                "+231", // Liberia
                "+232", // Sierra Leone
                "+244", // Angola
                "+252", // Somalia
                "+261", // Madagascar
                "+267", // Botswana
                "+269", // Comoros
                "+27", // South Africa — high wangiri volume
                "+355", // Albania
                "+370", // Lithuania
                "+371", // Latvia
                "+372", // Estonia
                "+373", // Moldova
                "+375", // Belarus
                "+7", // Russia/Kazakhstan
            )
    }
}
