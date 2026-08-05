package org.carlospinan.bloqueador.app.rules

import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.spam.BundledSpamProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * How the resolver hands an incoming number to the two rules that care what *form* it's in.
 *
 * Both of these were broken the same way: the resolver normalised the handle to bare digits
 * before consulting them, which throws away the leading "+". Country rules then attributed
 * national numbers to foreign countries, and the bundled spam list — whose entries are all
 * "+234"-style — could never match anything at all. Neither failure was visible from a unit
 * test of the component in isolation, so these assert through `evaluate`.
 */
class RulePrecedenceResolverNumberFormTest {
    private val emptyContext =
        ResolveContext(
            allowlistedNumbers = emptySet(),
            blockedNumbers = emptySet(),
            enabledPatterns = emptyList(),
            enabledCountryCodes = emptySet(),
        )

    // ---- country rules ----

    @Test
    fun anInternationalNumberFromABlockedCountryIsBlocked() =
        runTest {
            val ctx = emptyContext.copy(enabledCountryCodes = setOf("212"))

            val decision = RulePrecedenceResolver.evaluate("+212612345678", ctx)

            assertTrue(decision is RuleDecision.CountryBlock)
            assertEquals("212", decision.countryCode)
        }

    @Test
    fun aNationalNumberIsNotAttributedToACountryThatSharesItsLeadingDigits() =
        runTest {
            // "212" is Morocco's country code and also Manhattan's area code. Blocking Morocco
            // must not block New York.
            val morocco = emptyContext.copy(enabledCountryCodes = setOf("212"))
            assertTrue(RulePrecedenceResolver.evaluate("2125551234", morocco) is RuleDecision.DefaultAllow)

            // "91" is India's country code; this is a Madrid landline.
            val india = emptyContext.copy(enabledCountryCodes = setOf("91"))
            assertTrue(RulePrecedenceResolver.evaluate("912345678", india) is RuleDecision.DefaultAllow)
        }

    @Test
    fun theDoubleZeroAccessCodeIsStillTreatedAsInternational() =
        runTest {
            val ctx = emptyContext.copy(enabledCountryCodes = setOf("212"))

            val decision = RulePrecedenceResolver.evaluate("00212612345678", ctx)

            assertTrue(decision is RuleDecision.CountryBlock)
            assertEquals("212", decision.countryCode)
        }

    // ---- spam provider ----

    @Test
    fun theBundledProviderActuallyFiresThroughTheResolver() =
        runTest {
            val ctx = emptyContext.copy(spamProvider = BundledSpamProvider(), spamEnabled = true)

            // "+234" is in SPAM_PREFIXES. This came back DefaultAllow before the fix, meaning
            // every one of the bundled prefixes was inert in the shipping app.
            val decision = RulePrecedenceResolver.evaluate("+2348012345678", ctx)

            assertTrue(decision is RuleDecision.SpamHit)
            assertEquals("bundled", decision.source)
            assertTrue(decision.isBlocked)
        }

    @Test
    fun theProviderReceivesTheCanonicalInternationalForm() =
        runTest {
            var seen: String? = null
            val ctx =
                emptyContext.copy(
                    spamProvider =
                        object : org.carlospinan.bloqueador.app.spam.SpamProviderClient {
                            override suspend fun lookup(number: String) = null.also { seen = number }
                        },
                    spamEnabled = true,
                )

            RulePrecedenceResolver.evaluate("00 34 600-123-456", ctx)

            assertEquals("+34600123456", seen)
        }

    @Test
    fun aNationalHandleReachesTheProviderUnchangedRatherThanAsAFabricatedE164() =
        runTest {
            var seen: String? = null
            val ctx =
                emptyContext.copy(
                    spamProvider =
                        object : org.carlospinan.bloqueador.app.spam.SpamProviderClient {
                            override suspend fun lookup(number: String) = null.also { seen = number }
                        },
                    spamEnabled = true,
                )

            RulePrecedenceResolver.evaluate("2125551234", ctx)

            assertEquals("2125551234", seen, "we don't know this number's country, so we must not invent one")
        }

    @Test
    fun spainIsNeverFlaggedByTheBundledList() =
        runTest {
            val ctx = emptyContext.copy(spamProvider = BundledSpamProvider(), spamEnabled = true)

            assertTrue(RulePrecedenceResolver.evaluate("+34600123456", ctx) is RuleDecision.DefaultAllow)
        }
}
