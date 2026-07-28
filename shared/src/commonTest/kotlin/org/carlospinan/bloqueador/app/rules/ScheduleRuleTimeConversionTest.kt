package org.carlospinan.bloqueador.app.rules

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleRuleTimeConversionTest {

    @Test
    fun `minuteOfDayToHHmm midnight`() {
        assertEquals("00:00", minuteOfDayToHHmm(0))
    }

    @Test
    fun `minuteOfDayToHHmm noon`() {
        assertEquals("12:00", minuteOfDayToHHmm(720))
    }

    @Test
    fun `minuteOfDayToHHmm endOfDay`() {
        assertEquals("23:59", minuteOfDayToHHmm(1439))
    }

    @Test
    fun `minuteOfDayToHHmm singleDigitHour`() {
        assertEquals("07:30", minuteOfDayToHHmm(450))
    }

    @Test
    fun `isWithinWindow simple window in range`() {
        assertTrue(isWithinWindow(14, 9, 17))
    }

    @Test
    fun `isWithinWindow simple window before start`() {
        assertFalse(isWithinWindow(8, 9, 17))
    }

    @Test
    fun `isWithinWindow simple window after end`() {
        assertFalse(isWithinWindow(17, 9, 17))
    }

    @Test
    fun `isWithinWindow midnight crossing in range`() {
        assertTrue(isWithinWindow(2, 22, 7))
    }

    @Test
    fun `isWithinWindow midnight crossing out of range`() {
        assertFalse(isWithinWindow(12, 22, 7))
    }

    @Test
    fun `isWithinWindow midnight crossing at boundary`() {
        assertTrue(isWithinWindow(22, 22, 7))
        assertFalse(isWithinWindow(7, 22, 7))
    }

    companion object {
        private fun minuteOfDayToHHmm(minute: Int): String {
            val hour = (minute / 60).toString().padStart(2, '0')
            val min = (minute % 60).toString().padStart(2, '0')
            return "$hour:$min"
        }

        private fun isWithinWindow(
            minute: Int,
            startMinute: Int,
            endMinute: Int,
        ): Boolean =
            if (startMinute <= endMinute) {
                minute in startMinute until endMinute
            } else {
                minute >= startMinute || minute < endMinute
            }
    }
}
