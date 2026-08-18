package org.carlospinan.bloqueador.app.rules

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Why a call was allowed or blocked, as data rather than as a sentence.
 *
 * This used to be a `String?` assembled in [RuleDecision] — "Manually blocked", "Quiet hours",
 * "Country: Spain (34)". Two things were wrong with that. The app ships in four locales and
 * those words were English in all of them; and the string was written straight into
 * `CallLogEntry.rule_detail`, so even translating it later would have left every historical row
 * frozen in whatever language it was created in.
 *
 * Keeping the *reason* and dropping the *rendering* fixes both: the log stores this structure
 * (see [BlockReasonCodec]) and each platform turns it into words at the moment of display, in
 * whatever locale is current then. [Custom] is the exception — it carries a label the user typed
 * themselves, which is shown verbatim and never translated.
 */
@Serializable
sealed interface BlockReason {
    /** A label the user wrote on the rule. Shown as-is, in whatever language they wrote it. */
    @Serializable
    @SerialName("custom")
    data class Custom(
        val label: String,
    ) : BlockReason

    @Serializable
    @SerialName("manual")
    data object ManuallyBlocked : BlockReason

    @Serializable
    @SerialName("pattern")
    data class PatternMatch(
        val pattern: String,
    ) : BlockReason

    @Serializable
    @SerialName("country")
    data class Country(
        val countryCode: String,
        val countryName: String,
    ) : BlockReason

    @Serializable
    @SerialName("spam")
    data class Spam(
        val source: String,
        val confidencePercent: Int,
    ) : BlockReason

    @Serializable
    @SerialName("repeated")
    data class RepeatedCalls(
        val attempts: Int,
        val windowMinutes: Int,
    ) : BlockReason

    @Serializable
    @SerialName("quiet_hours")
    data object QuietHours : BlockReason

    @Serializable
    @SerialName("default_block")
    data object NoMatchingRule : BlockReason

    @Serializable
    @SerialName("allowed_repeated")
    data class AllowedAfterRepeatedAttempts(
        val attempts: Int,
    ) : BlockReason

    /** Let through untouched because the user had just called the emergency services. */
    @Serializable
    @SerialName("emergency_callback")
    data object EmergencyCallback : BlockReason
}

/**
 * Serialises a [BlockReason] for the `CallLogEntry.rule_detail` column.
 *
 * JSON rather than a delimiter-joined string because the payloads include user-typed labels and
 * raw patterns, which can contain any character a separator might have claimed.
 */
object BlockReasonCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(reason: BlockReason): String = json.encodeToString(BlockReason.serializer(), reason)

    /**
     * Rows written by earlier builds hold a plain English sentence rather than JSON, and rows
     * from a future build may hold a variant this one doesn't know. Both decode to [Custom] of
     * the raw text: worse than a translation, but it still tells the user what happened, which
     * beats a blank cell or a crash in the call log.
     */
    fun decode(stored: String?): BlockReason? {
        val raw = stored?.takeIf { it.isNotBlank() } ?: return null
        return try {
            json.decodeFromString(BlockReason.serializer(), raw)
        } catch (_: Exception) {
            BlockReason.Custom(raw)
        }
    }
}
