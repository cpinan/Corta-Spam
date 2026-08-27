package org.carlospinan.bloqueador.app.telecom

import org.carlospinan.bloqueador.app.call.CallUiPhase
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The reported bug: default action Block, the phone shows "Blocked call", and the call answers
 * itself — the user finds out when they hear it in progress.
 *
 * [PassthroughInCallService] ended every blocked call with `Call.reject()`, which Telecom applies
 * only to a ringing call. Anything that answered first — the ringing screen's own Answer button
 * reached by a cheek or a pocket, a headset button, the system UI — turned the rejection into a
 * no-op, and nothing looked again. The blocked-call notification was posted regardless, so the app
 * claimed to have blocked a call the user was connected to.
 *
 * The service itself cannot be unit tested: it is an `InCallService`, and `android.telecom.Call`
 * is final with no public constructor and no Robolectric shadow. The decision therefore lives in
 * [BlockedCallPolicy], over the same [CallUiPhase] the in-call screen uses.
 */
class BlockedCallPolicyTest {
    /** The ordinary case, and the only one the old code got right. */
    @Test
    fun `a ringing blocked call is rejected`() {
        assertEquals(
            BlockedCallPolicy.Termination.REJECT,
            BlockedCallPolicy.terminationFor(CallUiPhase.RINGING),
        )
    }

    /** The bug. `reject()` here does nothing, so the call has to be hung up instead. */
    @Test
    fun `a blocked call that is already connected is disconnected, not rejected`() {
        assertEquals(
            BlockedCallPolicy.Termination.DISCONNECT,
            BlockedCallPolicy.terminationFor(CallUiPhase.ACTIVE),
        )
    }

    /** Answered and parked is still answered. */
    @Test
    fun `a blocked call on hold is disconnected`() {
        assertEquals(
            BlockedCallPolicy.Termination.DISCONNECT,
            BlockedCallPolicy.terminationFor(CallUiPhase.HOLDING),
        )
    }

    /**
     * An unknown state is not a reason to leave a blocked call up. The safe answer is the one that
     * ends it, not the one that hopes.
     */
    @Test
    fun `a blocked call in an unrecognized state is disconnected`() {
        assertEquals(
            BlockedCallPolicy.Termination.DISCONNECT,
            BlockedCallPolicy.terminationFor(CallUiPhase.OTHER),
        )
    }

    /** Already going away: asking again would only race Telecom's own teardown. */
    @Test
    fun `a blocked call already ending is left alone`() {
        assertEquals(
            BlockedCallPolicy.Termination.ALREADY_ENDING,
            BlockedCallPolicy.terminationFor(CallUiPhase.DISCONNECTING),
        )
    }

    /**
     * The greeting deadlines bracket a real greeting rather than cutting one short: the start
     * deadline is the shorter of the two, and the ceiling is above the ~35 s a maximum-length
     * script takes to read aloud.
     */
    @Test
    fun `the greeting deadlines leave room for a real greeting`() {
        assert(BlockedCallPolicy.GREETING_START_TIMEOUT_MILLIS < BlockedCallPolicy.GREETING_MAX_MILLIS)
        assert(BlockedCallPolicy.GREETING_MAX_MILLIS >= 45_000L)
    }
}
