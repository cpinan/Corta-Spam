package org.carlospinan.bloqueador.app.telecom

import android.os.Build
import android.telecom.Call
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The truth table behind "may this app screen this call".
 *
 * An `InCallService` is handed calls in both directions. Everything the rule engine does assumes
 * the incoming one, so getting this wrong does not fail quietly: a blocklisted number the user
 * dials, or any number at all inside a quiet-hours window, would have the call they placed ended
 * by their own phone app.
 */
class CallDirectionPolicyTest {
    private val q = Build.VERSION_CODES.Q

    @Test
    fun `an incoming call on API 29 and above is screened`() {
        assertTrue(
            CallDirectionPolicy.isIncoming(
                sdkInt = q,
                callDirection = Call.Details.DIRECTION_INCOMING,
                state = Call.STATE_RINGING,
            ),
        )
    }

    /** The call the user placed. Screening it would run their blocklist against their own dialling. */
    @Test
    fun `an outgoing call on API 29 and above is not screened`() {
        assertFalse(
            CallDirectionPolicy.isIncoming(
                sdkInt = q,
                callDirection = Call.Details.DIRECTION_OUTGOING,
                state = Call.STATE_CONNECTING,
            ),
        )
        assertFalse(
            CallDirectionPolicy.isIncoming(
                sdkInt = 34,
                callDirection = Call.Details.DIRECTION_OUTGOING,
                state = Call.STATE_DIALING,
            ),
        )
    }

    /**
     * The state is not the authority once the direction is available. An outgoing call that has
     * somehow been reported as RINGING is still outgoing, and the direction says so.
     */
    @Test
    fun `direction beats state when direction is available`() {
        assertFalse(
            CallDirectionPolicy.isIncoming(
                sdkInt = 34,
                callDirection = Call.Details.DIRECTION_OUTGOING,
                state = Call.STATE_RINGING,
            ),
        )
        assertTrue(
            CallDirectionPolicy.isIncoming(
                sdkInt = 34,
                callDirection = Call.Details.DIRECTION_INCOMING,
                state = Call.STATE_CONNECTING,
            ),
        )
    }

    @Test
    fun `an unknown direction on API 29 and above is not screened`() {
        assertFalse(
            CallDirectionPolicy.isIncoming(
                sdkInt = 34,
                callDirection = Call.Details.DIRECTION_UNKNOWN,
                state = Call.STATE_RINGING,
            ),
        )
    }

    /** Below API 29 there is no direction, and Telecom adds an incoming call already ringing. */
    @Test
    fun `below API 29 a ringing call is screened`() {
        assertTrue(
            CallDirectionPolicy.isIncoming(
                sdkInt = Build.VERSION_CODES.P,
                callDirection = Call.Details.DIRECTION_UNKNOWN,
                state = Call.STATE_RINGING,
            ),
        )
    }

    @Test
    fun `below API 29 a call being dialled is not screened`() {
        listOf(Call.STATE_CONNECTING, Call.STATE_DIALING, Call.STATE_ACTIVE).forEach { state ->
            assertFalse(
                CallDirectionPolicy.isIncoming(
                    sdkInt = Build.VERSION_CODES.O,
                    callDirection = Call.Details.DIRECTION_UNKNOWN,
                    state = state,
                ),
                "state $state should not be screened below API 29",
            )
        }
    }

    /**
     * The two mistakes are not the same size: failing to screen one call is a nuisance, ending a
     * call the user placed is the phone breaking. An unrecognised state resolves to "not
     * incoming" for that reason.
     */
    @Test
    fun `an unrecognised state below API 29 is not screened`() {
        assertFalse(
            CallDirectionPolicy.isIncoming(
                sdkInt = Build.VERSION_CODES.O,
                callDirection = Call.Details.DIRECTION_UNKNOWN,
                state = -1,
            ),
        )
    }
}
