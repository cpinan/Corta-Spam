package org.carlospinan.bloqueador.app.telecom

import kotlin.random.Random

/**
 * The rules of "waste their time": an answered spam call kept busy with canned lines until the
 * caller gives up or the cap is hit.
 *
 * Pure so the parts that decide how long a call stays open can be tested without a Telecom stack.
 * The lines themselves are the `stall_line_N` resources, `1..LINE_COUNT`.
 */
object StallPolicy {
    const val LINE_COUNT = 8

    /**
     * Hard ceiling on one stalled call. The call is connected, on the loudspeaker, with the
     * microphone open the whole time, so it must always end; five minutes is long enough to cost a
     * call centre real time and short enough that a forgotten phone is not left live.
     */
    const val MAX_MILLIS = 5 * 60_000L

    /** Silence between lines, so the caller can talk and the exchange does not sound looped. */
    const val MIN_GAP_MILLIS = 3_000L
    const val MAX_GAP_MILLIS = 7_000L

    /** A 1-based line number, never the one just played. */
    fun nextLine(
        previous: Int?,
        random: Random,
    ): Int {
        val pick = random.nextInt(1, LINE_COUNT + 1)
        if (pick != previous) return pick
        return pick % LINE_COUNT + 1
    }

    fun gapMillis(random: Random): Long = random.nextLong(MIN_GAP_MILLIS, MAX_GAP_MILLIS + 1)
}
