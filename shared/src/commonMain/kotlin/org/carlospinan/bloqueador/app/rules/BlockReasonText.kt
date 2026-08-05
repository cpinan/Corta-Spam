package org.carlospinan.bloqueador.app.rules

import androidx.compose.runtime.Composable
import cortaspam.shared.generated.resources.Res
import cortaspam.shared.generated.resources.reason_allowed_after_repeated
import cortaspam.shared.generated.resources.reason_country
import cortaspam.shared.generated.resources.reason_manually_blocked
import cortaspam.shared.generated.resources.reason_no_matching_rule
import cortaspam.shared.generated.resources.reason_pattern_match
import cortaspam.shared.generated.resources.reason_quiet_hours
import cortaspam.shared.generated.resources.reason_repeated_calls
import cortaspam.shared.generated.resources.reason_spam
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Renders a [BlockReason] in the reader's current locale.
 *
 * The counterpart on the Android side is `BlockReasonStrings`, which does the same job from a
 * `Context` for notifications posted outside a Composable.
 */
@Composable
fun blockReasonText(reason: BlockReason): String =
    when (reason) {
        // The user's own words for their own rule -- translating them would be wrong.
        is BlockReason.Custom -> reason.label
        is BlockReason.ManuallyBlocked -> stringResource(Res.string.reason_manually_blocked)
        is BlockReason.PatternMatch -> stringResource(Res.string.reason_pattern_match, reason.pattern)
        is BlockReason.Country -> stringResource(Res.string.reason_country, reason.countryName, reason.countryCode)
        is BlockReason.Spam -> stringResource(Res.string.reason_spam, reason.source, reason.confidencePercent)
        is BlockReason.RepeatedCalls -> stringResource(Res.string.reason_repeated_calls, reason.attempts, reason.windowMinutes)
        is BlockReason.QuietHours -> stringResource(Res.string.reason_quiet_hours)
        is BlockReason.NoMatchingRule -> stringResource(Res.string.reason_no_matching_rule)
        // A plural, not a format string: "Called 1 times" is wrong in every locale we ship.
        is BlockReason.AllowedAfterRepeatedAttempts ->
            pluralStringResource(Res.plurals.reason_allowed_after_repeated, reason.attempts, reason.attempts)
    }

/** Convenience for a stored `CallLogEntry.rule_detail`; null when the row recorded no reason. */
@Composable
fun storedBlockReasonText(stored: String?): String? = BlockReasonCodec.decode(stored)?.let { blockReasonText(it) }
