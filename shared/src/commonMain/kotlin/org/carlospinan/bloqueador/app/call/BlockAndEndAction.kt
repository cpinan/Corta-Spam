package org.carlospinan.bloqueador.app.call

/** What a "hang up & block" / "decline & block" tap should actually do to the call on screen. */
enum class BlockAndEndAction {
    DECLINE,
    HANG_UP,

    /** The call this was drawn for is no longer the one on screen; touch nothing. */
    NONE,
}

/**
 * Decides [BlockAndEndAction] from what is live right now, not from the snapshot the button was
 * rendered with.
 *
 * The tap and the render it came from are not atomic: a second call arriving as call waiting
 * promotes itself to the call on screen the instant it arrives, synchronously and ahead of
 * Compose's next recomposition — so a tap already in flight can land after the promotion. Ending
 * "whatever call is on screen now" would then end a call the user never touched, while the call
 * they actually meant to end stays connected. This is checked by number, the same identity this
 * layer already uses elsewhere for exactly this reason (see `InCallState.setDisplayName`,
 * `setRepeatedCallAttempts`) — there is no stronger identity available above the platform `Call`
 * object, which this layer never sees.
 *
 * The block-list write itself needs no such guard: adding a number to the list is safe regardless
 * of which call, if any, is still up, so it always goes ahead — only the termination action does not.
 */
fun blockAndEndAction(
    requestedNumber: String,
    liveNumber: String?,
    livePhase: CallUiPhase?,
): BlockAndEndAction {
    if (liveNumber == null || liveNumber != requestedNumber) return BlockAndEndAction.NONE
    return if (livePhase == CallUiPhase.RINGING) BlockAndEndAction.DECLINE else BlockAndEndAction.HANG_UP
}
