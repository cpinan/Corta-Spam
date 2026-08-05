package org.carlospinan.bloqueador.app.spam

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [BundledSpamProvider] is the only [SpamProviderClient] bound today, and it decides whether a
 * call gets a SPAM verdict entirely from an on-device list. Worth pinning because a change to
 * that list changes who gets blocked, with no network call or server-side flag to roll back.
 */
class BundledSpamProviderTest {
    private val provider = BundledSpamProvider()

    @Test
    fun aKnownSpamPrefixIsFlaggedWithTheBundledSource() =
        runTest {
            val result = provider.lookup("+2348012345678")

            assertEquals(true, result?.isSpam)
            assertEquals(0.7f, result?.confidence)
            assertEquals("bundled", result?.source)
        }

    @Test
    fun prefixesMatchOnStartsWithNotEquality() =
        runTest {
            // "+7" is a single-digit-country prefix, so anything Russian/Kazakh matches.
            assertTrue(provider.lookup("+79991234567")?.isSpam == true)
            assertTrue(provider.lookup("+27831234567")?.isSpam == true)
        }

    @Test
    fun anUnlistedCountryIsNotFlagged() =
        runTest {
            // Spain, the app's primary market, must never be flagged by the bundled list.
            assertNull(provider.lookup("+34600123456"))
            assertNull(provider.lookup("+15551234567"))
        }

    @Test
    fun surroundingWhitespaceIsTrimmedBeforeMatching() =
        runTest {
            assertTrue(provider.lookup("  +2348012345678  ")?.isSpam == true)
        }

    @Test
    fun aNationalFormatNumberIsNotMatched() =
        runTest {
            // Every entry is stored in +E.164 form and matching is a raw startsWith. The
            // resolver is what guarantees this method actually receives that form -- see
            // SpamProviderClient.lookup's contract and RulePrecedenceResolverSpamTest.
            assertNull(provider.lookup("2348012345678"))
        }

    @Test
    fun aNumberIsNeverFlaggedOnItsDigitShapeAlone() =
        runTest {
            // There is no shape-based pattern list any more. The one that used to exist was
            // unreachable dead code, and a live "contains a run of zeros" rule would reject real
            // subscribers -- these three are ordinary numbers and must all come back clean.
            assertNull(provider.lookup("+34000123456"))
            assertNull(provider.lookup("+34600000123"))
            assertNull(provider.lookup("+15550000000"))
        }
}
