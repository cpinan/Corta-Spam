package org.carlospinan.bloqueador.app.telecom

import android.content.Context
import org.carlospinan.bloqueador.app.R
import org.carlospinan.bloqueador.app.rules.BlockReason

/**
 * Renders a [BlockReason] for notification text.
 *
 * The Compose counterpart is `blockReasonText`, but notifications are built from a
 * [android.telecom.Call.Callback] and a [android.app.Service], where no Composable scope exists
 * — hence the second, `Context`-based implementation. The strings themselves are duplicated
 * between `androidApp/src/main/res` and `shared/src/commonMain/composeResources`, which is this
 * project's standing arrangement for text needed by both worlds.
 */
object BlockReasonStrings {
    fun format(
        context: Context,
        reason: BlockReason,
    ): String =
        when (reason) {
            // The user's own label for their own rule; shown exactly as they typed it.
            is BlockReason.Custom -> reason.label
            is BlockReason.ManuallyBlocked -> context.getString(R.string.reason_manually_blocked)
            is BlockReason.PatternMatch -> context.getString(R.string.reason_pattern_match, reason.pattern)
            is BlockReason.Country ->
                context.getString(R.string.reason_country, reason.countryName, reason.countryCode)
            is BlockReason.Spam ->
                context.getString(R.string.reason_spam, reason.source, reason.confidencePercent)
            is BlockReason.RepeatedCalls ->
                context.getString(R.string.reason_repeated_calls, reason.attempts, reason.windowMinutes)
            is BlockReason.QuietHours -> context.getString(R.string.reason_quiet_hours)
            is BlockReason.EmergencyCallback -> context.getString(R.string.reason_emergency_callback)
            is BlockReason.NoMatchingRule -> context.getString(R.string.reason_no_matching_rule)
            // A plural, not a format string: "Called 1 times" is wrong in every shipped locale.
            is BlockReason.AllowedAfterRepeatedAttempts ->
                context.resources.getQuantityString(
                    R.plurals.reason_allowed_after_repeated,
                    reason.attempts,
                    reason.attempts,
                )
        }
}
