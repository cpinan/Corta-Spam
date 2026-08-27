package org.carlospinan.bloqueador.app.telecom

import android.telecom.Call
import org.carlospinan.bloqueador.app.call.CallUiPhase

/**
 * Telecom's call states, narrowed to what this app has to decide between.
 *
 * `STATE_SIMULATED_RINGING` is a ringing call: it is what Telecom reports while a call screening
 * service is deciding, and to the person holding the phone it is a call coming in. The constant is
 * a compile-time `int`, so naming it costs nothing on older releases.
 *
 * Everything not listed falls to [CallUiPhase.OTHER], which is a state the app knows nothing
 * about rather than one it can assume is safe.
 *
 * Shared rather than private to [InCallState] because [BlockedCallPolicy] decides how to end a
 * call from the same three-way split, and two copies of this `when` would drift.
 */
internal fun Int.toCallUiPhase(): CallUiPhase =
    when (this) {
        Call.STATE_RINGING, Call.STATE_SIMULATED_RINGING -> CallUiPhase.RINGING
        Call.STATE_DIALING, Call.STATE_CONNECTING -> CallUiPhase.DIALING
        Call.STATE_ACTIVE -> CallUiPhase.ACTIVE
        Call.STATE_HOLDING -> CallUiPhase.HOLDING
        Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> CallUiPhase.DISCONNECTING
        else -> CallUiPhase.OTHER
    }
