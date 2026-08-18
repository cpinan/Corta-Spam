package org.carlospinan.bloqueador.app.call

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The call timer's arithmetic, away from a screen.
 *
 * Worth its own test because the boundaries are where a timer looks broken to the person watching
 * it: 59 to 60 seconds, and the minute that has to stop being a minute once there is an hour.
 */
class CallDurationFormatTest {
    @Test
    fun `a call that has just connected reads as zero`() {
        assertEquals("00:00", formatCallDuration(0))
    }

    @Test
    fun `seconds are padded so the timer does not jump about`() {
        assertEquals("00:07", formatCallDuration(7))
    }

    @Test
    fun `the last second before a minute is still seconds`() {
        assertEquals("00:59", formatCallDuration(59))
    }

    @Test
    fun `a minute rolls over`() {
        assertEquals("01:00", formatCallDuration(60))
        assertEquals("01:14", formatCallDuration(74))
    }

    /** No hour field until there is an hour: a two-minute call is not `0:02:14`. */
    @Test
    fun `an hour appears only once there is one`() {
        assertEquals("59:59", formatCallDuration(3599))
        assertEquals("1:00:00", formatCallDuration(3600))
        assertEquals("2:05:03", formatCallDuration(7503))
    }

    /**
     * `connectTimeMillis` is wall clock, so a clock that moves backwards mid-call can hand this a
     * negative number. A timer reading `-00:03` is a bug report; zero is merely wrong for a moment.
     */
    @Test
    fun `a clock that went backwards does not print a negative timer`() {
        assertEquals("00:00", formatCallDuration(-5))
    }
}
