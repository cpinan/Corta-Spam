package org.carlospinan.bloqueador.app.telecom

import org.carlospinan.bloqueador.app.call.CallUiPhase

/**
 * Whether the in-call screen should be blanked while something is close to the proximity sensor.
 *
 * Split out of [InCallActivity] for the reason [RingerPolicy], [NotificationPolicy] and
 * [CallDirectionPolicy] were: the activity cannot be constructed in a unit test, and holding a
 * `PROXIMITY_SCREEN_OFF_WAKE_LOCK` at the wrong moment is not a cosmetic mistake — it is a screen
 * that goes black while the user is trying to press something.
 *
 * The problem it solves is the other direction. A phone held to the ear rests a cheek on whatever
 * is on screen, and this screen has a hang-up button on it — plus, since mute and speaker were
 * added, two more controls a cheek can reach. Every other phone app turns the screen off for
 * exactly this reason. The lock is sensor-driven: it blanks only while something is actually near,
 * so holding it costs nothing when the phone is on a table.
 */
object ProximityPolicy {
    /**
     * True while the phone is somewhere a face might be.
     *
     * [CallUiPhase.ACTIVE] and [CallUiPhase.DIALING] both count: waiting for the other end to pick
     * up is spent with the handset against the ear just as much as talking is.
     *
     * [CallUiPhase.RINGING] deliberately does not. An incoming call has to be answered or
     * declined, and blanking the screen would hide the two buttons that do it — a phone ringing in
     * a pocket would otherwise arrive at the ear already unanswerable. [CallUiPhase.HOLDING] and
     * [CallUiPhase.DISCONNECTING] are screens the user is looking at rather than listening to, and
     * [CallUiPhase.OTHER] is unknown, which is the case for leaving the screen on.
     *
     * [speakerOn] switches it off wholesale: a call on the loudspeaker is one the phone is not
     * against a face for, and blanking it there would black out the screen every time a hand
     * passed over the device.
     */
    fun shouldBlankScreen(
        phase: CallUiPhase,
        speakerOn: Boolean,
    ): Boolean {
        if (speakerOn) return false
        return phase == CallUiPhase.ACTIVE || phase == CallUiPhase.DIALING
    }
}
