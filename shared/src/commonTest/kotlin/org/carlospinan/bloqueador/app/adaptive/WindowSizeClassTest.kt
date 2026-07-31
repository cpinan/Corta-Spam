package org.carlospinan.bloqueador.app.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WindowSizeClassTest {
    @Test
    fun `enum has three values in expected ordinal order`() {
        val values = WindowSizeClass.entries
        assertEquals(3, values.size)
        assertEquals(WindowSizeClass.Compact, values[0])
        assertEquals(WindowSizeClass.Medium, values[1])
        assertEquals(WindowSizeClass.Expanded, values[2])
    }

    @Test
    fun `ordinal ordering respects size hierarchy`() {
        assertTrue(WindowSizeClass.Compact.ordinal < WindowSizeClass.Medium.ordinal)
        assertTrue(WindowSizeClass.Medium.ordinal < WindowSizeClass.Expanded.ordinal)
    }
}
