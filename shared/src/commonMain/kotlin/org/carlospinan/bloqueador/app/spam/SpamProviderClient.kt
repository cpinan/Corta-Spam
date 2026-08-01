package org.carlospinan.bloqueador.app.spam

/** Result from a spam provider lookup. */
data class SpamResult(
    val isSpam: Boolean,
    val confidence: Float,
    val source: String,
)

/**
 * Pluggable spam-provider interface. The only bound implementation
 * ([org.carlospinan.bloqueador.app.spam.BundledSpamProvider]) is a fully
 * offline, local heuristic — no data leaves the device today, but the
 * interface exists so a future implementation could query an external
 * database without touching call sites.
 */
interface SpamProviderClient {
    suspend fun lookup(number: String): SpamResult?
}
