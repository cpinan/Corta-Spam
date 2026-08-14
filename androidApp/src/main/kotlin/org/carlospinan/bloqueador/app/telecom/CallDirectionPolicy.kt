package org.carlospinan.bloqueador.app.telecom

import android.os.Build
import android.telecom.Call

/**
 * Whether a call Telecom just handed over is one this app is allowed to screen.
 *
 * Split out of [PassthroughInCallService] for the reason [RingerPolicy] and [NotificationPolicy]
 * were: the service cannot be constructed in a unit test, and this decision is the difference
 * between blocking a spammer and hanging up on the user.
 *
 * An `InCallService` receives **every** call, in both directions — that is the price of owning the
 * in-call UI. The rule engine only ever meant incoming ones. Screening an outgoing call runs the
 * user's own blocklist against the number they just dialled, and a match ends the call they
 * placed; a quiet-hours rule, which blocks everything not allowlisted, would end *every* outgoing
 * call for the length of the window. With the auto-responder on it is worse than a hang-up: the
 * app answers the call and plays the user's own greeting into it.
 */
object CallDirectionPolicy {
    /**
     * True when [callDirection] / [state] describe a call arriving from someone else.
     *
     * [Call.Details.getCallDirection] is the authoritative answer and exists from API 29. Below
     * that the state at the moment of `onCallAdded` is the signal: Telecom adds an incoming call
     * already `STATE_RINGING`, while an outgoing one starts `STATE_CONNECTING` or `STATE_DIALING`.
     *
     * Deliberately conservative in the other direction: an unrecognised state on an old API is
     * treated as **not** incoming, so an unexpected value can only ever cost screening on one
     * call. Guessing "incoming" would risk terminating a call the user placed, and the two
     * mistakes are not the same size.
     */
    fun isIncoming(
        sdkInt: Int,
        callDirection: Int,
        state: Int,
    ): Boolean =
        if (sdkInt >= Build.VERSION_CODES.Q) {
            callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            state == Call.STATE_RINGING
        }

    fun isIncoming(call: Call): Boolean =
        isIncoming(
            sdkInt = Build.VERSION.SDK_INT,
            callDirection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    call.details?.callDirection ?: Call.Details.DIRECTION_UNKNOWN
                } else {
                    Call.Details.DIRECTION_UNKNOWN
                },
            state = call.state,
        )
}
