package org.carlospinan.bloqueador.app.contacts

import org.carlospinan.bloqueador.app.rules.PhoneNumberParser

/**
 * The contact's name, or [number] itself when nobody in the address book claims it.
 *
 * Probes every form the number may be filed under rather than its digits alone. Contacts are
 * saved the way they are dialled and calls arrive in E.164, so comparing digits meant screens
 * full of bare numbers for people who were in the address book all along — while the
 * incoming-call notification showed the name correctly, because that path goes through the
 * platform's own PhoneLookup.
 *
 * The probe has to be the plural one: [ContactsGateway.contactNames] keys its map by *every*
 * [PhoneNumberParser.comparisonKeys] form of each contact, so asking it for one key can miss an
 * entry that is genuinely there.
 *
 * Shared rather than per-screen on purpose. This existed twice — correct in the call log, still
 * single-key in the block lists — and the block lists showed bare numbers for months after the
 * call log was fixed, because nothing tied the two copies together.
 */
fun contactDisplayName(
    number: String,
    contactNames: Map<String, String>,
): String =
    PhoneNumberParser
        .comparisonKeys(number)
        .firstNotNullOfOrNull { contactNames[it] }
        ?: number

/**
 * Whether the address book claims [number].
 *
 * Deliberately the same probe as [contactDisplayName] rather than a second opinion. The platform's
 * own `ContactsContract.PhoneLookup` looks like the obvious way to ask this and is not: it matches
 * through the provider's normalized `data4` column, which is computed from the device's default
 * region, so a contact saved nationally does not match an E.164 call unless the phone's region
 * happens to agree. On an emulator with no SIM `data4` is null and *every* contact reads as a
 * stranger. Deriving the country code from the number itself, which is what comparisonKeys does,
 * has no region to be wrong about.
 */
fun isKnownContact(
    number: String,
    contactNames: Map<String, String>,
): Boolean = PhoneNumberParser.comparisonKeys(number).any { it in contactNames }
