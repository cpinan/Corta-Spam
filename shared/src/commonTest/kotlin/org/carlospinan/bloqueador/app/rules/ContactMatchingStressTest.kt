package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.settings.DefaultAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The contacts allowlist under volume: ten real address-book entries against a hundred strangers
 * built to look exactly like them.
 *
 * This is the algorithm behind the report *"the number was not in their agenda and it still got
 * through"*. Contacts are merged into the allowlist by [PhoneNumberParser.sameNumber], and that
 * comparison has to be loose enough to recognise a contact saved `611 99 88 77` in a call arriving
 * as `+34611998877` — while being tight enough that a stranger who merely resembles one is not
 * waved through. The existing tests pick a handful of pairs by hand; the failure mode this guards
 * against is the one a handful of pairs cannot show, where the rule is *almost* right and lets a
 * whole shape of number through.
 *
 * **Asserted through `evaluate`, not through `sameNumber`.** The comparison has been made inert
 * before by a caller that normalised the number first (see the contact-matching history), and a
 * direct unit test of the predicate passed the whole time that was true. `defaultAction` is BLOCK
 * so the two outcomes are unmissable: a contact must come back [RuleDecision.Allowlist], and a
 * stranger must come back [RuleDecision.DefaultBlock]. A false match reads as "Allowlist where
 * DefaultBlock was expected", which is the user's report in one line.
 */
class ContactMatchingStressTest {
    /**
     * One address-book entry.
     *
     * [nationalSignificant] is the number without its country code — the part that identifies the
     * subscriber — and is kept separately because both the strangers and the alternative arrival
     * forms are built from it. For the UK the national form adds a trunk `0` the E.164 form drops;
     * for Italy the `0` is part of the national significant number and survives into E.164. Those
     * two are in here precisely because they pull in opposite directions.
     */
    private data class AddressBookEntry(
        val label: String,
        val savedAs: String,
        val countryCode: String,
        val nationalSignificant: String,
        val nationalForm: String,
    ) {
        val e164: String get() = "+$countryCode$nationalSignificant"
    }

    /**
     * Ten contacts, saved the inconsistent way real address books hold them: some international,
     * some national, some with the punctuation the user typed.
     *
     * Deliberately similar to *each other* as well as to the strangers below — two Spanish mobiles
     * sharing their first three digits, two New York numbers sharing their first seven. A matcher
     * that is too loose fails by confusing one contact for another just as readily as by admitting
     * a stranger.
     */
    private val contacts =
        listOf(
            AddressBookEntry("ES mobile saved nationally", "611 99 88 77", "34", "611998877", "611998877"),
            AddressBookEntry("ES mobile sharing its prefix", "611 99 77 88", "34", "611997788", "611997788"),
            AddressBookEntry("ES mobile saved in E.164", "+34600111222", "34", "600111222", "600111222"),
            AddressBookEntry("ES landline", "912345678", "34", "912345678", "912345678"),
            // The trunk zero is dropped by E.164 here...
            AddressBookEntry("UK mobile with a trunk zero", "07700 900123", "44", "7700900123", "07700900123"),
            // ...and kept by it here. Both must work off one rule.
            AddressBookEntry("IT landline keeping its zero", "06 1234 5678", "39", "0612345678", "0612345678"),
            AddressBookEntry("PE mobile saved in E.164", "+51 987 654 321", "51", "987654321", "987654321"),
            AddressBookEntry("US number saved nationally", "(212) 555-1234", "1", "2125551234", "2125551234"),
            AddressBookEntry("US number sharing its exchange", "+1 212 555 9876", "1", "2125559876", "2125559876"),
            AddressBookEntry("IN mobile saved in E.164", "+91 98123 45678", "91", "9812345678", "9812345678"),
        )

    private val addressBook = contacts.map { it.savedAs }.toSet()

    private fun context() =
        ResolveContext(
            allowlistedNumbers = emptySet(),
            contactNumbers = addressBook,
            blockedNumbers = emptySet(),
            enabledPatterns = emptyList(),
            enabledCountryCodes = emptySet(),
            // Anything that is not recognised as a contact is blocked, so "was this matched?" and
            // "did this get through?" are the same question — which is how the report was phrased.
            defaultAction = DefaultAction.BLOCK,
        )

    /**
     * A hundred numbers that are not in the address book, each one a near-miss of a contact.
     *
     * Built by changing only the **last one or two digits** of a contact's national significant
     * number, which is what makes the set safe to assert on rather than merely plausible. Every
     * key [PhoneNumberParser.comparisonKeys] can produce — the full digit string, the national
     * significant number, the trunk-stripped form — ends in those digits, and keys are compared
     * for equality rather than by suffix. Change the tail and no key of the stranger can equal any
     * key of the contact, whatever country code either of them carries. So these are different
     * subscribers by construction, not by assumption.
     *
     * The country code is left alone for the same reason: changing *only* the country code of a
     * nationally-saved contact hits a documented limit of the matcher rather than a bug, and it is
     * asserted on its own below instead of being smuggled in here.
     */
    private fun strangers(): List<Pair<String, AddressBookEntry>> =
        contacts.flatMap { entry ->
            val nsn = entry.nationalSignificant
            val head = nsn.dropLast(2)
            val lastTwo = nsn.takeLast(2).toInt()

            (1..10).map { step ->
                // Adding 1..10 to the final two digits, modulo 100, never lands back on the
                // original and never carries into the digits above it.
                val mutatedTail = ((lastTwo + step) % 100).toString().padStart(2, '0')
                val mutated = head + mutatedTail
                val number =
                    when (step % 5) {
                        0 -> "+${entry.countryCode}$mutated"
                        1 -> mutated
                        2 -> "00${entry.countryCode}$mutated"
                        3 -> "+${entry.countryCode} ${mutated.chunked(3).joinToString(" ")}"
                        else -> mutated.chunked(3).joinToString("-")
                    }
                number to entry
            }
        }

    @Test
    fun theStrangerSetIsAHundredNumbersNoneOfWhichIsAContact() {
        val generated = strangers()

        assertEquals(100, generated.size, "the stress set must be a hundred numbers")
        assertEquals(
            100,
            generated.map { PhoneNumberParser.normalizeForComparison(it.first) }.distinct().size,
            "the hundred strangers must be a hundred distinct subscribers",
        )

        // The premise of the whole test, checked rather than assumed: no stranger shares a digit
        // string with any contact in any form the address book holds.
        val contactDigits =
            contacts
                .flatMap { listOf(it.savedAs, it.e164, it.nationalForm, it.nationalSignificant) }
                .map { PhoneNumberParser.normalizeForComparison(it) }
                .toSet()
        val collisions =
            generated
                .map { PhoneNumberParser.normalizeForComparison(it.first) }
                .filter { it in contactDigits }
        assertEquals(emptyList(), collisions, "the generator produced a number that IS a contact")
    }

    @Test
    fun everyContactIsAllowedWhateverFormTheCallArrivesIn() =
        runTest {
            val ctx = context()
            val failures = mutableListOf<String>()

            for (entry in contacts) {
                // The four ways the same subscriber reaches the phone: canonical international,
                // the 00 access code, national as dialled locally, and international with the
                // spacing a carrier or a paste can introduce.
                val arrivals =
                    listOf(
                        entry.e164,
                        "00${entry.countryCode}${entry.nationalSignificant}",
                        entry.nationalForm,
                        "+${entry.countryCode} ${entry.nationalSignificant}",
                    )
                for (arrival in arrivals) {
                    val decision = RulePrecedenceResolver.evaluate(arrival, ctx)
                    if (decision !is RuleDecision.Allowlist) {
                        failures += "${entry.label}: $arrival -> $decision (expected Allowlist)"
                    }
                }
            }

            assertEquals(emptyList(), failures, "a contact was not recognised and would be blocked")
        }

    @Test
    fun aHundredStrangersThatLookLikeContactsAreNoneOfThem() =
        runTest {
            val ctx = context()
            val leaked = mutableListOf<String>()

            for ((number, entry) in strangers()) {
                val decision = RulePrecedenceResolver.evaluate(number, ctx)
                if (decision !is RuleDecision.DefaultBlock) {
                    leaked += "$number matched ${entry.label} (${entry.savedAs}) -> $decision"
                }
            }

            // Named for the report: this is what "it was not in my agenda and it still rang" is.
            assertEquals(emptyList(), leaked, "strangers were treated as contacts and let through")
        }

    @Test
    fun aNumberEndingInAContactsNumberIsNotThatContact() =
        runTest {
            // Suffix matching was considered for the national/international problem and rejected,
            // because this is what it costs: any number long enough to end in a contact's digits
            // would be allowlisted. Asserted so a future "just compare the last N digits" has to
            // delete a test that says why not.
            val ctx = context()

            for (entry in contacts) {
                val longer = "+${entry.countryCode}999${entry.nationalSignificant}"
                assertTrue(
                    RulePrecedenceResolver.evaluate(longer, ctx) is RuleDecision.DefaultBlock,
                    "$longer was matched to ${entry.label} on its suffix alone",
                )
            }
        }

    /**
     * A real dialling code that is not [countryCode].
     *
     * It has to be one [PhoneNumberParser] actually knows. An invented code like `+99` is not
     * attributed to any country, so the parser produces no national significant number for it and
     * the number fails to match for the wrong reason — which is how the first version of the two
     * tests below passed without exercising anything.
     */
    private fun anotherRealCountryCode(countryCode: String): String =
        when (countryCode) {
            "51" -> "34"
            else -> "51"
        }

    @Test
    fun twoInternationalNumbersDifferingOnlyByCountryCodeAreDifferentPeople() =
        runTest {
            // Both sides state their country, so the codes are facts rather than guesses and the
            // national form must not bridge them. This is the half of the documented limit that
            // IS enforced.
            val ctx = context()
            val savedInternationally = contacts.filter { it.savedAs.startsWith("+") }

            assertTrue(savedInternationally.isNotEmpty())
            for (entry in savedInternationally) {
                val foreignTwin = "+${anotherRealCountryCode(entry.countryCode)}${entry.nationalSignificant}"
                assertTrue(
                    RulePrecedenceResolver.evaluate(foreignTwin, ctx) is RuleDecision.DefaultBlock,
                    "$foreignTwin was matched to ${entry.label}",
                )
            }
        }

    /**
     * The known limit, asserted rather than hidden.
     *
     * A contact saved without a country code does not state one, so a call that *does* state one
     * can still be the same subscriber — that is the whole reason the national form is allowed to
     * bridge them, and it is what makes a contact saved `611 99 88 77` reachable at all. The cost
     * is that a foreign number with an identical national significant number is admitted too.
     *
     * This is a real way a stranger can get through, and if the reporter's caller turns out to be
     * a foreign number colliding with a nationally-saved contact, **this test is the explanation
     * and it will need to change**. Closing it needs a region to canonicalise against — a SIM or
     * locale lookup and a per-country trunk-prefix rule — which was considered and rejected. It is
     * pinned here so the trade is visible and a change to it is deliberate.
     */
    @Test
    fun aContactSavedNationallyIsAlsoMatchedByTheSameNationalNumberAbroad() =
        runTest {
            val ctx = context()
            val savedNationally = contacts.first { !it.savedAs.startsWith("+") }

            val foreignTwin =
                "+${anotherRealCountryCode(savedNationally.countryCode)}${savedNationally.nationalSignificant}"
            val decision = RulePrecedenceResolver.evaluate(foreignTwin, ctx)

            assertTrue(
                decision is RuleDecision.Allowlist,
                "the documented limit changed: $foreignTwin no longer matches ${savedNationally.savedAs}",
            )
        }
}
