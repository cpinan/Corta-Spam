package org.carlospinan.bloqueador.app.keypad

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the reported defect: with a number already in the field and the caret moved to its front, a
 * dial-pad key still landed at the *end*. The field held a plain String, which carries no caret,
 * so every control that typed into it could only append -- and the commonest correction a dialer
 * is asked for, adding a country or area code in front, could not be made at all.
 */
class NumberEntryTest {
    @Test
    fun `a key typed at the front of the number lands at the front`() {
        val entry = typeInto(text = "5551234", selectionStart = 0, selectionEnd = 0, typed = "9")

        assertEquals("95551234", entry.text)
        // And the caret follows what was typed, so the next key continues the prefix instead of
        // typing over it backwards: "9" then "1" is "91...", never "19...".
        assertEquals(1, entry.caret)
    }

    @Test
    fun `a key typed in the middle lands in the middle`() {
        val entry = typeInto(text = "551234", selectionStart = 2, selectionEnd = 2, typed = "0")

        assertEquals("5501234", entry.text)
        assertEquals(3, entry.caret)
    }

    @Test
    fun `a key typed at the end still appends`() {
        val entry = typeInto(text = "5551234", selectionStart = 7, selectionEnd = 7, typed = "8")

        assertEquals("55512348", entry.text)
        assertEquals(8, entry.caret)
    }

    @Test
    fun `typing over a selection replaces it`() {
        val entry = typeInto(text = "5551234", selectionStart = 0, selectionEnd = 3, typed = "9")

        assertEquals("91234", entry.text)
        assertEquals(1, entry.caret)
    }

    /**
     * A selection is reported by the field, and a stale one that outruns the text would take the
     * screen down inside a keystroke rather than at a boundary anyone would look at.
     */
    @Test
    fun `a selection past the end of the text does not crash`() {
        val entry = typeInto(text = "55", selectionStart = 9, selectionEnd = 12, typed = "1")

        assertEquals("551", entry.text)
        assertEquals(3, entry.caret)
    }

    @Test
    fun `delete removes the character before the caret rather than the last one`() {
        val entry = deleteBackwards(text = "5551234", selectionStart = 3, selectionEnd = 3)

        assertEquals("551234", entry.text)
        assertEquals(2, entry.caret)
    }

    @Test
    fun `delete with the caret at the front removes nothing`() {
        val entry = deleteBackwards(text = "5551234", selectionStart = 0, selectionEnd = 0)

        assertEquals("5551234", entry.text)
        assertEquals(0, entry.caret)
    }

    @Test
    fun `delete removes the whole selection`() {
        val entry = deleteBackwards(text = "5551234", selectionStart = 1, selectionEnd = 4)

        assertEquals("5234", entry.text)
        assertEquals(1, entry.caret)
    }

    @Test
    fun `delete on an empty number is a no-op`() {
        val entry = deleteBackwards(text = "", selectionStart = 0, selectionEnd = 0)

        assertEquals("", entry.text)
        assertEquals(0, entry.caret)
    }
}
