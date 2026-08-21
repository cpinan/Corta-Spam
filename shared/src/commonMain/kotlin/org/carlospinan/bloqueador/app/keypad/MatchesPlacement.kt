package org.carlospinan.bloqueador.app.keypad

/**
 * Which side of the number field the search results open on, and how tall they may be.
 *
 * In pixels and free of Compose types, because this arithmetic has been wrong twice and both times
 * it took a device to notice: once the popup covered the screen's title and clipped a result row
 * through the middle of its text, and once it did the same after the content was packed to the
 * bottom of the window. Neither was visible to a component test — semantics do not change when one
 * window overlaps another — but both are two numbers and a comparison, which is exactly what a
 * unit test can hold still.
 */
internal data class MatchesPlacement(
    val openUpwards: Boolean,
    val maxHeightPx: Float,
)

/**
 * The side with more room wins, and the height never exceeds that room.
 *
 * **Never exceeding it is the rule that matters.** The screen decides the side, not the popup's own
 * position provider, because what has to be avoided below the field is the dial pad — which the
 * provider cannot see, and which a popup does not merely cover but takes the touches from. And a
 * minimum height cannot be honoured against a window that does not have it: a popup told to be
 * taller than its space is pinned to the edge of the screen and sliced. Short results scroll;
 * sliced ones lie about what they contain.
 *
 * [minPx] is therefore a preference, not a floor: it is what the results are given when there is
 * room for it, and what they lose first when there is not.
 */
internal fun matchesPlacement(
    spaceAbovePx: Float,
    spaceBelowPx: Float,
    gapPx: Float,
    minPx: Float,
    maxPx: Float,
): MatchesPlacement {
    val openUpwards = spaceAbovePx >= spaceBelowPx
    val available = (if (openUpwards) spaceAbovePx else spaceBelowPx) - gapPx
    val height =
        when {
            // Nothing has been measured yet -- the first frame, before the field reports where it
            // is. Anything else here would draw a sliver and then jump.
            available <= 0f -> maxPx
            available < minPx -> available
            else -> available.coerceAtMost(maxPx)
        }
    return MatchesPlacement(openUpwards = openUpwards, maxHeightPx = height)
}
