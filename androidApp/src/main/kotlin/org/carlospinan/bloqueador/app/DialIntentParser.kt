package org.carlospinan.bloqueador.app

import android.content.Intent
import android.net.Uri

/**
 * Reads the number out of the intents a default dialer is expected to answer.
 *
 * Kept as a pure function over (action, data) rather than something that takes an [Intent]: the
 * manifest has declared `ACTION_DIAL` filters since the app first claimed `ROLE_DIALER`, nothing
 * ever read them, and the number was silently dropped on every `tel:` link the user tapped. A
 * decision no test could reach is how that shipped.
 */
object DialIntentParser {
    /**
     * The dialled number, or null when this intent carries none.
     *
     * `ACTION_DIAL` with no data is legitimate — it means "open the dialer" — and returns null so
     * the caller shows an empty keypad rather than a request for the empty string.
     */
    fun numberFrom(
        action: String?,
        data: Uri?,
    ): String? {
        if (action != Intent.ACTION_DIAL && action != Intent.ACTION_VIEW) return null
        if (data == null) return null
        if (!data.scheme.equals("tel", ignoreCase = true)) return null

        // Uri.getSchemeSpecificPart is already percent-decoded, which matters: a '#' typed into a
        // dialler arrives as %23 and would otherwise be read as the start of a fragment and lost.
        val number = data.schemeSpecificPart?.trim().orEmpty()
        return number.ifEmpty { null }
    }

    fun numberFrom(intent: Intent?): String? = numberFrom(intent?.action, intent?.data)
}
