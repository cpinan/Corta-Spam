package org.carlospinan.bloqueador.app.telecom

import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Covers the part of [InCallState] reachable without an `android.telecom.Call`.
 *
 * **What is not covered, and why:** `attach`/`detach`, the `Call.Callback` phase mapping, and
 * `answer`/`decline`/`hangUp` actually reaching a call all need a real `android.telecom.Call`.
 * That class is final, has no public constructor, and no Robolectric shadow ships for it, so the
 * only way to supply one is a mocking library — which `docs/SPEC.md` §6 rules out for this
 * project (hand-written fakes only, so the same tests can run on Kotlin/Native). Making that
 * path testable would mean introducing an interface between the Telecom callbacks and this
 * object, which is a design change, not a test change.
 *
 * What is left is still worth pinning: every method here is called from a `BroadcastReceiver`
 * ([CallActionReceiver]) and a `Service` ([PassthroughInCallService]), which can both fire after
 * the call they refer to has gone away. A crash on that path would surface as the phone app
 * dying mid-call.
 */
class InCallStateTest {
    @Test
    fun `state is null when no call is attached`() {
        assertNull(InCallState.state.value)
    }

    @Test
    fun `call actions with no attached call are no-ops rather than crashes`() {
        // Reached when a notification action is tapped just after the call ends: the receiver
        // has no idea the call is gone. These must not throw.
        InCallState.answer()
        InCallState.decline()
        InCallState.hangUp()

        assertNull(InCallState.state.value)
    }

    /**
     * The name lookup runs off the call-setup path, so it can land after the call it was started
     * for has ended — and with no call attached there is nothing to name.
     */
    @Test
    fun `a name resolved after the call ended is dropped`() {
        InCallState.setDisplayName("+34600123456", "Ana Torres")

        assertNull(InCallState.state.value)
    }

    @Test
    fun `repeated-attempt info arriving before a call is dropped, not buffered`() {
        // The count is filed against the call with that number, and with no calls attached
        // there is none to file it against. PassthroughInCallService therefore has to attach the
        // call before reporting the attempt count, or the ringing screen shows no hint.
        InCallState.setRepeatedCallAttempts("+34600123456", 4)

        assertNull(InCallState.state.value)
    }
}
