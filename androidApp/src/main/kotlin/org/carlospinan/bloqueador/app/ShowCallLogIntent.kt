package org.carlospinan.bloqueador.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

/**
 * The intent behind a finished-call notification's *body*.
 *
 * Its buttons already act on the number without opening anything. The body did nothing at all:
 * it had no content intent, so tapping the notification that told the user about a call simply
 * dismissed it, and the call log it was reporting stayed two navigation steps away.
 *
 * Modelled on [DialIntentParser] -- a pure function over (action, number) so the decision is
 * testable without an Intent, plus the builder that constructs the real one.
 */
object ShowCallLogIntent {
    const val ACTION = "org.carlospinan.bloqueador.app.ACTION_SHOW_CALL_LOG"
    const val EXTRA_NUMBER = "org.carlospinan.bloqueador.app.EXTRA_CALL_LOG_NUMBER"

    fun numberFrom(
        action: String?,
        number: String?,
    ): String? {
        if (action != ACTION) return null
        return number?.trim()?.ifEmpty { null }
    }

    fun numberFrom(intent: Intent?): String? = numberFrom(intent?.action, intent?.getStringExtra(EXTRA_NUMBER))

    /**
     * [Intent.setData] carries the number a second time, and it is not redundant: PendingIntent
     * equality ignores extras, so without a per-number URI the notification for one caller and
     * the notification for the next would share a single PendingIntent -- and `FLAG_UPDATE_CURRENT`
     * would silently repoint the older notification at the newer caller. The same trap is why the
     * action buttons carry one. The extra is what is read; the URI is what keeps them distinct.
     */
    fun forNumber(
        context: Context,
        number: String,
    ): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(ACTION)
            .setData("cortaspam://call-log/${Uri.encode(number)}".toUri())
            .putExtra(EXTRA_NUMBER, number)
            .addFlags(
                // The activity is `standard` and already on a task of its own. Without CLEAR_TOP
                // a tap would stack a second copy of the app on top of the running one, and the
                // user would back out of the call log into the call log.
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
}
