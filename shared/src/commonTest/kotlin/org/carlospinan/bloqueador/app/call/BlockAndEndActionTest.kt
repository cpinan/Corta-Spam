package org.carlospinan.bloqueador.app.call

import kotlin.test.Test
import kotlin.test.assertEquals

class BlockAndEndActionTest {
    @Test
    fun ringingCallStillOnScreenIsDeclined() {
        assertEquals(
            BlockAndEndAction.DECLINE,
            blockAndEndAction(
                requestedNumber = "+34611998877",
                liveNumber = "+34611998877",
                livePhase = CallUiPhase.RINGING,
            ),
        )
    }

    @Test
    fun activeCallStillOnScreenIsHungUp() {
        assertEquals(
            BlockAndEndAction.HANG_UP,
            blockAndEndAction(
                requestedNumber = "+34611998877",
                liveNumber = "+34611998877",
                livePhase = CallUiPhase.ACTIVE,
            ),
        )
    }

    /** Call waiting promoted a different call before the tap landed: touch nothing. */
    @Test
    fun aDifferentCallOnScreenIsLeftAlone() {
        assertEquals(
            BlockAndEndAction.NONE,
            blockAndEndAction(
                requestedNumber = "+34611998877",
                liveNumber = "+34600111222",
                livePhase = CallUiPhase.ACTIVE,
            ),
        )
    }

    /** The call ended entirely before the tap landed: there is nothing left to touch. */
    @Test
    fun noLiveCallIsLeftAlone() {
        assertEquals(
            BlockAndEndAction.NONE,
            blockAndEndAction(
                requestedNumber = "+34611998877",
                liveNumber = null,
                livePhase = null,
            ),
        )
    }
}
