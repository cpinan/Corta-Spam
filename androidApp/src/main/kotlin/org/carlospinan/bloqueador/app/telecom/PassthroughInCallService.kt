package org.carlospinan.bloqueador.app.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService

/**
 * M1 scope: pure pass-through, no blocking logic (docs/MILESTONES.md M1).
 *
 * This is the mandatory in-call UI/audio contract for the default-dialer role
 * (docs/SPEC.md §1/§4). Real SIM connections still come from Android's own
 * telephony ConnectionService -- this class does not reimplement call
 * handling, it only owns the UI/audio contract.
 *
 * Deliberately does NOT write call-log entries itself: verified on-device
 * that Android's Telecom stack (`com.android.server.telecom`) logs every
 * call automatically regardless of which app holds the default-dialer role
 * -- a manual `ContentResolver.insert(CallLog.Calls.CONTENT_URI, ...)` here
 * produced a confirmed duplicate row per call (the system's own row carries
 * `phone_account_address` = the SIM's number; ours didn't and was strictly
 * redundant). Third-party default dialers are not expected to log calls
 * themselves.
 */
class PassthroughInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        InCallState.attach(call)
        startActivity(
            Intent(this, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        InCallState.detach(call)
    }
}
