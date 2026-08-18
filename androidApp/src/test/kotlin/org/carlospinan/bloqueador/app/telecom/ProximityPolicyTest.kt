package org.carlospinan.bloqueador.app.telecom

import org.carlospinan.bloqueador.app.call.CallUiPhase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The decision behind the proximity wake lock, away from the activity that holds it.
 *
 * Both directions matter and they fail differently. Not blanking leaves a cheek resting on a
 * hang-up button; blanking at the wrong moment takes the screen away from someone trying to press
 * one. The second is the reason RINGING is tested explicitly.
 */
class ProximityPolicyTest {
    @Test
    fun `a connected call on the earpiece blanks the screen`() {
        assertTrue(ProximityPolicy.shouldBlankScreen(CallUiPhase.ACTIVE, speakerOn = false))
    }

    /** Waiting for the other end to answer is spent against the ear too. */
    @Test
    fun `a dialling call blanks the screen`() {
        assertTrue(ProximityPolicy.shouldBlankScreen(CallUiPhase.DIALING, speakerOn = false))
    }

    /**
     * The dangerous direction. Answer and Decline are the whole screen for a ringing call, and a
     * phone that rang in a pocket would reach the ear already unanswerable.
     */
    @Test
    fun `a ringing call never blanks the screen`() {
        assertFalse(ProximityPolicy.shouldBlankScreen(CallUiPhase.RINGING, speakerOn = false))
    }

    /** On the loudspeaker the phone is on a table, and a hand passing over it is not a face. */
    @Test
    fun `the loudspeaker switches it off whatever the call is doing`() {
        assertFalse(ProximityPolicy.shouldBlankScreen(CallUiPhase.ACTIVE, speakerOn = true))
        assertFalse(ProximityPolicy.shouldBlankScreen(CallUiPhase.DIALING, speakerOn = true))
    }

    /** Screens the user is looking at rather than listening to. */
    @Test
    fun `a held or ending call leaves the screen on`() {
        assertFalse(ProximityPolicy.shouldBlankScreen(CallUiPhase.HOLDING, speakerOn = false))
        assertFalse(ProximityPolicy.shouldBlankScreen(CallUiPhase.DISCONNECTING, speakerOn = false))
    }

    /**
     * OTHER is every state this screen has no specific handling for, and it is deliberately the
     * safe answer rather than the convenient one: an unknown state must not be able to black out
     * the screen. Same reasoning as [CallDirectionPolicy]'s unrecognised-state default.
     */
    @Test
    fun `an unrecognised state leaves the screen on`() {
        assertFalse(ProximityPolicy.shouldBlankScreen(CallUiPhase.OTHER, speakerOn = false))
    }
}
