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
}
