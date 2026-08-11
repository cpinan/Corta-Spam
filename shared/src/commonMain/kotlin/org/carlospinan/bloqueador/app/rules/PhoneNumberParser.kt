package org.carlospinan.bloqueador.app.rules

object PhoneNumberParser {
    /** Longest-first so NANP entries like "1264" (Anguilla) win over the bare "1" (US/Canada). */
    private val knownCodes: List<String> by lazy {
        COUNTRIES.map { it.code }.distinct().sortedByDescending { it.length }
    }

    /** E.164 caps a full number at 15 digits, country code included. */
    private const val MAX_E164_DIGITS = 15

    /**
     * Canonical `+<digits>` form, or null when [number] was not written in international form.
     *
     * The distinction is the whole point. A national-format number carries no country
     * information at all, and guessing one from its leading digits is wrong far more often than
     * it is right: "2125551234" is a Manhattan landline, but its first three digits are also
     * Morocco's country code, and "912345678" is a Madrid landline whose first two are India's.
     * Reading either as international meant that blocking Morocco silently blocked New York.
     *
     * Both international spellings are accepted: a leading `+`, and the `00` access code used
     * across most of Europe. Formatting characters are ignored, so `"+34 600 123 456"` and
     * `"+34-600-123-456"` both canonicalise to `"+34600123456"`.
     */
    fun toE164OrNull(number: String): String? {
        val trimmed = number.trim()
        val digits =
            when {
                trimmed.startsWith("+") -> normalizeForComparison(trimmed)
                trimmed.startsWith("00") -> normalizeForComparison(trimmed).drop(2)
                else -> return null
            }
        if (digits.isEmpty() || digits.length > MAX_E164_DIGITS) return null
        return "+$digits"
    }

    /**
     * The calling country's dialling code, or null when [number] isn't in international form or
     * doesn't begin with a code we know.
     *
     * A match also needs at least 4 digits of subscriber number after the code, so a short code
     * or a truncated handle isn't attributed to a country on the strength of its prefix alone.
     */
    fun parseCountryCode(number: String): String? {
        val digits = toE164OrNull(number)?.removePrefix("+") ?: return null
        return knownCodes.firstOrNull { code ->
            digits.startsWith(code) && digits.length >= code.length + 4
        }
    }

    /**
     * Strip all non-digit characters for fuzzy comparison across different
     * number formats (E.164, local, formatted, etc.).
     */
    fun normalizeForComparison(number: String): String = number.filter { it.isDigit() }

    /**
     * Every string [number] may legitimately be recognised by. Two numbers refer to the same
     * subscriber when their key sets intersect.
     *
     * Digit equality alone is not enough, and the gap is the common case rather than an edge one.
     * People save contacts the way they dial them — `611 99 88 77` — while a call arrives in E.164
     * as `+34611998877`. Those differ by the country code, so they never compared equal, and the
     * damage was not cosmetic: `RulePrecedenceResolver` merges contacts into the allowlist by this
     * comparison, so a real contact saved nationally was **not allowlisted** and could be blocked
     * by quiet hours, a country rule or the default action, silently.
     *
     * The fix deliberately does *not* guess a region. An international number already states its
     * own country code, so stripping that code yields the national significant number, which is
     * exactly the form a contact card holds. Guessing a default region instead would need a
     * SIM/locale lookup, an ISO-to-dialling-code table this app does not have, and a trunk-prefix
     * rule that differs per country — dropping a leading zero is right for the UK and wrong for
     * Italy, whose E.164 form keeps it. Deriving the code from the number sidesteps all three.
     *
     * Keys returned:
     *  - the full digit string (`34611998877`) — an exact match still wins, and two international
     *    numbers from different countries are still told apart by it
     *  - the national significant number when the country code is known (`611998877`)
     *  - for a national-format input, the digits with one trunk `0` removed, which is what makes
     *    a UK contact saved `07700900123` meet `+447700900123`
     *
     * **Known limit:** a national-format contact carries no country information, so
     * `+34611998877` and `+51611998877` both meet a contact saved as `611998877`. Matching the
     * whole national number rather than a digit suffix keeps that narrow — a stranger must share
     * every digit, not the last seven — but it is a real trade and the reason the full-digit key
     * is kept alongside.
     */
    fun comparisonKeys(number: String): Set<String> {
        val digits = normalizeForComparison(number)
        if (digits.isEmpty()) return emptySet()

        val keys = mutableSetOf(digits)
        val national = nationalSignificantOrNull(number)
        if (national != null) keys += national
        return keys
    }

    /**
     * The number without its country code (international input) or without a trunk `0` (national
     * input), or null when neither applies. This is the form a contact card usually holds.
     */
    private fun nationalSignificantOrNull(number: String): String? {
        val e164Digits = toE164OrNull(number)?.removePrefix("+")
        if (e164Digits != null) {
            val code = knownCodes.firstOrNull { e164Digits.startsWith(it) && e164Digits.length >= it.length + 4 }
            return code?.let { e164Digits.drop(it.length) }
        }
        val digits = normalizeForComparison(number)
        // A trunk prefix is an extra key, never a replacement, so a country whose E.164 form keeps
        // the leading zero (Italy) still matches on its untouched digits.
        return if (digits.length > 1 && digits.startsWith("0")) digits.drop(1) else null
    }

    /**
     * True when [a] and [b] can be the same subscriber.
     *
     * The national form is allowed to bridge the two **only when at least one side does not state
     * its country**. When both are international the country codes are known facts, so
     * `+34611998877` and `+51611998877` are different people even though their national numbers
     * are identical — matching them would allowlist a foreign stranger on the strength of a
     * coincidence. This asymmetry is the whole rule; comparing key sets blindly gets it wrong.
     */
    fun sameNumber(
        a: String,
        b: String,
    ): Boolean {
        val digitsA = normalizeForComparison(a)
        val digitsB = normalizeForComparison(b)
        if (digitsA.isEmpty() || digitsB.isEmpty()) return false
        if (digitsA == digitsB) return true

        val aIsInternational = toE164OrNull(a) != null
        val bIsInternational = toE164OrNull(b) != null
        if (aIsInternational && bIsInternational) {
            // Both know their country; only the full E.164 digits can make them the same.
            return toE164OrNull(a) == toE164OrNull(b)
        }

        // At least one side is national, so the national form may bridge them. Intersect the
        // candidate sets rather than reducing each side to one string: the trunk zero has to stay
        // available as well as stripped, because Italy's E.164 form keeps it ("+39 06...") while
        // the UK's drops it ("+44 7700..." for a contact saved "07700...").
        val keysA = comparisonKeys(a)
        return comparisonKeys(b).any { it in keysA }
    }
}
