package org.carlospinan.bloqueador.app.spam

import kotlinx.coroutines.test.runTest
import org.carlospinan.bloqueador.app.rules.ResolveContext
import org.carlospinan.bloqueador.app.rules.RuleDecision
import org.carlospinan.bloqueador.app.rules.RulePrecedenceResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpamProviderClientTest {
    private val emptyContext =
        ResolveContext(
            allowlistedNumbers = emptySet(),
            blockedNumbers = emptySet(),
            enabledPatterns = emptyList(),
            enabledCountryCodes = emptySet(),
        )

    @Test
    fun noOpProvider_returnsNull() =
        runTest {
            val provider = NoOpSpamProvider()
            assertNull(provider.lookup("+34600123456"))
        }

    @Test
    fun fakeSpamProvider_returnsSpamResult() =
        runTest {
            val provider =
                object : SpamProviderClient {
                    override suspend fun lookup(number: String): SpamResult? =
                        if (number == "+34600123456") {
                            SpamResult(isSpam = true, confidence = 0.95f, source = "test")
                        } else {
                            null
                        }
                }
            val result = provider.lookup("+34600123456")
            assertTrue(result != null)
            assertTrue(result!!.isSpam)
            assertEquals(0.95f, result.confidence)
            assertEquals("test", result.source)
        }

    @Test
    fun resolver_spamHit_whenProviderEnabled() =
        runTest {
            val provider =
                object : SpamProviderClient {
                    override suspend fun lookup(number: String): SpamResult? = SpamResult(isSpam = true, confidence = 0.8f, source = "test")
                }
            val ctx =
                emptyContext.copy(
                    spamProvider = provider,
                    spamEnabled = true,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.SpamHit)
            assertTrue(decision.isBlocked)
            assertEquals(0.8f, (decision as RuleDecision.SpamHit).confidence)
            assertEquals("SPAM", decision.ruleTypeTag)
        }

    @Test
    fun resolver_spamDisabled_fallsThrough() =
        runTest {
            val provider =
                object : SpamProviderClient {
                    override suspend fun lookup(number: String): SpamResult? = SpamResult(isSpam = true, confidence = 0.8f, source = "test")
                }
            val ctx =
                emptyContext.copy(
                    spamProvider = provider,
                    spamEnabled = false,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.DefaultAllow)
        }

    @Test
    fun resolver_providerReturnsNull_fallsThrough() =
        runTest {
            val provider =
                object : SpamProviderClient {
                    override suspend fun lookup(number: String): SpamResult? = null
                }
            val ctx =
                emptyContext.copy(
                    spamProvider = provider,
                    spamEnabled = true,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.DefaultAllow)
        }

    @Test
    fun resolver_providerReturnsNotSpam_fallsThrough() =
        runTest {
            val provider =
                object : SpamProviderClient {
                    override suspend fun lookup(number: String): SpamResult? =
                        SpamResult(isSpam = false, confidence = 0.1f, source = "test")
                }
            val ctx =
                emptyContext.copy(
                    spamProvider = provider,
                    spamEnabled = true,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.DefaultAllow)
        }

    @Test
    fun resolver_noProvider_fallsThrough() =
        runTest {
            val ctx =
                emptyContext.copy(
                    spamProvider = null,
                    spamEnabled = true,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertTrue(decision is RuleDecision.DefaultAllow)
        }

    @Test
    fun spamHit_blockReason_containsSourceAndConfidence() =
        runTest {
            val provider =
                object : SpamProviderClient {
                    override suspend fun lookup(number: String): SpamResult? =
                        SpamResult(isSpam = true, confidence = 0.75f, source = "known-spam")
                }
            val ctx =
                emptyContext.copy(
                    spamProvider = provider,
                    spamEnabled = true,
                )
            val decision = RulePrecedenceResolver.evaluate("+34600123456", ctx)
            assertEquals("Spam (known-spam, 75%)", decision.blockReason)
        }
}
