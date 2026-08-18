package org.carlospinan.bloqueador.app.rules

/**
 * Numbers that mean "emergency services" in the countries this app ships a language for.
 *
 * Used in exactly one direction: recognising a number the user has just **dialled**, so an
 * incoming call shortly afterwards can be treated as the callback. It is never used to classify an
 * incoming caller ID — a callback almost never arrives from the number that was dialled, and a
 * blocker that trusted caller ID to say "this is the emergency services" would be trivially
 * spoofable.
 *
 * **This is deliberately not a complete world list, and it cannot be one.** The complete answer is
 * `TelephonyManager.isEmergencyNumber`, which reads the list the SIM and network actually
 * provisioned — and which requires `READ_PHONE_STATE`, a permission this app does not declare and
 * has a manifest comment explaining why. So the platform's own signal
 * (`PROPERTY_EMERGENCY_CALLBACK_MODE`) is the primary one, and this list is the fallback for
 * networks that never enter callback mode. A number missing from here costs the exemption on that
 * call, not correctness anywhere else.
 *
 * Matched whole, never as a prefix: `1120000` is an ordinary subscriber number.
 */
object EmergencyNumbers {
    /** Grouped by why each is here, so an addition has to justify itself the same way. */
    private val WELL_KNOWN: Set<String> =
        setOf(
            // Reachable on any GSM handset regardless of country or SIM.
            "112",
            "911",
            // United Kingdom, Ireland, Hong Kong, Malaysia, and others.
            "999",
            // Australia, and New Zealand's separate number.
            "000",
            "111",
            // Police in Germany, Japan and China; fire/ambulance in Japan, Korea and China.
            "110",
            "119",
            // Italy's ambulance line.
            "118",
            // India: police, fire, ambulance, and the unified emergency response line.
            "100",
            "101",
            "102",
            "108",
            // Brazil: police, ambulance, fire.
            "190",
            "192",
            "193",
            // Peru: police, ambulance, fire.
            "105",
            "106",
            "116",
        )

    /**
     * Whether [number] is one of [WELL_KNOWN], ignoring the spacing and punctuation a dialler may
     * have inserted.
     *
     * A leading `+` disqualifies it: an emergency number is dialled bare, and `+34112` is a
     * perfectly ordinary international number that happens to end in one of these.
     */
    fun isWellKnown(number: String): Boolean {
        val trimmed = number.trim()
        if (trimmed.startsWith("+")) return false
        val digits = trimmed.filter { it.isDigit() }
        return digits.isNotEmpty() && digits in WELL_KNOWN
    }
}
