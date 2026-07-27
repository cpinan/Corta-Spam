package org.carlospinan.bloqueador.app.rules

object PhoneNumberParser {
    /** Longest-first so NANP entries like "1264" (Anguilla) win over the bare "1" (US/Canada). */
    private val knownCodes: List<String> by lazy {
        COUNTRIES.map { it.code }.distinct().sortedByDescending { it.length }
    }

    fun parseCountryCode(number: String): String? {
        val cleaned = number.trim().removePrefix("+")
        if (cleaned.isEmpty() || !cleaned.all { it.isDigit() } || cleaned.length > 15) return null

        return knownCodes.firstOrNull { code ->
            cleaned.startsWith(code) && cleaned.length >= code.length + 4
        }
    }
}
