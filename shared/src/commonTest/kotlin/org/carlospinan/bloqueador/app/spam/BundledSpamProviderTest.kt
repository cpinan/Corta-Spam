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
    fun aNumberWithoutTheLeadingPlusIsNotMatched() =
        runTest {
            // Every entry in the list is stored in +E.164 form and matching is a raw startsWith,
            // so a nationally-formatted number never hits the list.
            assertNull(provider.lookup("2348012345678"))
            assertNull(provider.lookup("002348012345678"))
        }

    @Test
    fun theBundledPatternListCannotMatchAnything() =
        runTest {
            // Documents dead code rather than asserting intent. The one SPAM_PATTERNS entry is
            // "+*000*", but this matcher (like RulePrecedenceResolver's) only understands a
            // leading and/or trailing star: it strips the outer stars and compares the remainder
            // literally, so the pattern becomes startsWith("+*000") -- which needs a literal '*'
            // inside a phone number. The 0.65-confidence branch is therefore unreachable, and no
            // "contains 000" number is flagged.
            assertNull(provider.lookup("+34000123456"))
            assertNull(provider.lookup("+34600000123"))
            assertNull(provider.lookup("+1000000000"))
        }
}
