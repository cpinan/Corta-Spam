package org.carlospinan.bloqueador.app.telecom

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StallPolicyTest {
    @Test
    fun `a line is never repeated back to back`() {
        val random = Random(7)
        var previous: Int? = null
        repeat(2_000) {
            val next = StallPolicy.nextLine(previous, random)
            assertNotEquals(previous, next)
            previous = next
        }
    }

    @Test
    fun `every line number stays in range and every line gets used`() {
        val random = Random(1)
        val seen = mutableSetOf<Int>()
        var previous: Int? = null
        repeat(2_000) {
            val next = StallPolicy.nextLine(previous, random)
            assertTrue(next in 1..StallPolicy.LINE_COUNT, "line $next out of range")
            seen += next
            previous = next
        }
        assertEquals((1..StallPolicy.LINE_COUNT).toSet(), seen)
    }

    @Test
    fun `the wrap-around pick is the one after the collision, not the collision`() {
        // A random that always returns the previous line forces the fallback path.
        val fixed =
            object : Random() {
                override fun nextBits(bitCount: Int) = 0

                override fun nextInt(
                    from: Int,
                    until: Int,
                ) = StallPolicy.LINE_COUNT
            }
        assertEquals(1, StallPolicy.nextLine(StallPolicy.LINE_COUNT, fixed))
    }

    @Test
    fun `gaps stay inside the configured window`() {
        val random = Random(3)
        repeat(1_000) {
            val gap = StallPolicy.gapMillis(random)
            assertTrue(gap in StallPolicy.MIN_GAP_MILLIS..StallPolicy.MAX_GAP_MILLIS)
        }
    }

    @Test
    fun `the cap is shorter than nothing would be and longer than a greeting`() {
        assertTrue(StallPolicy.MAX_MILLIS > BlockedCallPolicy.GREETING_MAX_MILLIS)
    }
}
