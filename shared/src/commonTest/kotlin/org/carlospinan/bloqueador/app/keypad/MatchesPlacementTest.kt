package org.carlospinan.bloqueador.app.keypad

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The two clipping defects this pins were both found by looking at a phone, and neither was
 * visible to a component test: a popup covering the field's label or slicing a row through its
 * text changes nothing about the semantics tree. They were arithmetic, though, and arithmetic
 * holds still.
 */
class MatchesPlacementTest {
    private val gap = 24f
    private val min = 264f
    private val max = 1020f

    @Test
    fun `the side with more room wins`() {
        assertTrue(
            matchesPlacement(spaceAbovePx = 900f, spaceBelowPx = 100f, gapPx = gap, minPx = min, maxPx = max)
                .openUpwards,
        )
        assertFalse(
            matchesPlacement(spaceAbovePx = 100f, spaceBelowPx = 900f, gapPx = gap, minPx = min, maxPx = max)
                .openUpwards,
        )
    }

    /**
     * The regression seen on a razr 50 ultra: with the content packed to the bottom of the window
     * the room above the field fell under the preferred height, and a popup taller than its space
     * is pinned to the edge of the screen and sliced through a row of text.
     */
    @Test
    fun `a window with less room than the preferred height gets a shorter popup rather than a clipped one`() {
        val placement =
            matchesPlacement(spaceAbovePx = 200f, spaceBelowPx = 0f, gapPx = gap, minPx = min, maxPx = max)

        assertEquals(176f, placement.maxHeightPx)
        assertTrue(placement.maxHeightPx <= 200f - gap, "the popup must fit the space it was given")
    }

    /** The gap that keeps the popup off the field's floating label comes out of the height. */
    @Test
    fun `the anchor gap is subtracted from the room available`() {
        val placement =
            matchesPlacement(spaceAbovePx = 500f, spaceBelowPx = 0f, gapPx = gap, minPx = min, maxPx = max)

        assertEquals(500f - gap, placement.maxHeightPx)
    }

    @Test
    fun `a tall window is capped rather than filled`() {
        val placement =
            matchesPlacement(spaceAbovePx = 4000f, spaceBelowPx = 0f, gapPx = gap, minPx = min, maxPx = max)

        assertEquals(max, placement.maxHeightPx)
    }

    /**
     * The first frame, before the field has reported where it is. Drawing a sliver and then
     * jumping to full height is worse than starting at the cap and settling down.
     */
    @Test
    fun `nothing measured yet falls back to the cap`() {
        val placement =
            matchesPlacement(spaceAbovePx = 0f, spaceBelowPx = 0f, gapPx = gap, minPx = min, maxPx = max)

        assertEquals(max, placement.maxHeightPx)
    }

    @Test
    fun `space smaller than the gap alone still does not produce a negative height`() {
        val placement =
            matchesPlacement(spaceAbovePx = 10f, spaceBelowPx = 0f, gapPx = gap, minPx = min, maxPx = max)

        assertTrue(placement.maxHeightPx > 0f, "got ${placement.maxHeightPx}")
    }
}
