package org.carlospinan.bloqueador.app.spam

/** Result from a spam provider lookup. */
data class SpamResult(
    val isSpam: Boolean,
    val confidence: Float,
    val source: String,
)

/**
 * Pluggable spam-provider interface. Implementations query external
 * databases to check if a number is known spam.
 *
 * The no-op default always returns null (unknown) — no data leaves
 * the device unless the user configures a real provider.
 */
interface SpamProviderClient {
    suspend fun lookup(number: String): SpamResult?
}

/** No-op default: always returns null (unknown). */
class NoOpSpamProvider : SpamProviderClient {
    override suspend fun lookup(number: String): SpamResult? = null
}
