package org.carlospinan.bloqueador.app.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WindowSizeClassBoundaryTest {

    @Test
    fun `ordinal order is Compact before Medium before Expanded`() {
        assertTrue(WindowSizeClass.Compact.ordinal < WindowSizeClass.Medium.ordinal)
        assertTrue(WindowSizeClass.Medium.ordinal < WindowSizeClass.Expanded.ordinal)
    }

    @Test
    fun `all three values are distinct`() {
        val values = WindowSizeClass.entries
        assertEquals(3, values.size)
        assertTrue(values[0] != values[1])
        assertTrue(values[1] != values[2])
        assertTrue(values[0] != values[2])
    }

    @Test
    fun `Compact is the first enum value`() {
        assertEquals(WindowSizeClass.Compact, WindowSizeClass.entries[0])
    }

    @Test
    fun `Expanded is the last enum value`() {
        assertEquals(WindowSizeClass.Expanded, WindowSizeClass.entries[2])
    }
}
