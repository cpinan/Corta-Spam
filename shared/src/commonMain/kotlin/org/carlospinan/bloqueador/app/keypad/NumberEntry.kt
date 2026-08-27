package org.carlospinan.bloqueador.app.keypad

/**
 * What the dial pad's field holds: the number, and where the caret sits in it.
 *
 * The field used to be a plain `String`, which cannot say where the caret is -- so every control
 * that types into it could only append. Putting the caret in front of a number already typed and
 * tapping a key put the digit at the *end*, which makes the one correction a dialer is asked for
 * most often -- a missing country or area code at the front -- impossible without deleting the
 * whole number and starting again. Reported from the app.
 *
 * [caret] is a position *between* characters: 0 is before the first, `text.length` after the last.
 */
internal data class NumberEntry(
    val text: String,
    val caret: Int,
)

/**
 * Types [typed] in at the caret, replacing the selection `[selectionStart, selectionEnd)` when the
 * user has one, and leaves the caret after what was just typed -- so a second key lands next to
 * the first rather than jumping back to where the caret started.
 *
 * The bounds are coerced rather than trusted: a selection is reported by the field, and a stale
 * one that outruns the text it came from would otherwise crash the screen mid-keystroke.
 */
internal fun typeInto(
    text: String,
    selectionStart: Int,
    selectionEnd: Int,
    typed: String,
): NumberEntry {
    val start = selectionStart.coerceIn(0, text.length)
    val end = selectionEnd.coerceIn(0, text.length)
    val from = minOf(start, end)
    val to = maxOf(start, end)
    return NumberEntry(
        text = text.replaceRange(from, to, typed),
        caret = from + typed.length,
    )
}

/**
 * Deletes the selection when there is one, otherwise the single character before the caret --
 * which is what the delete key on every phone's dialer does, and what "delete the last character"
 * only happened to be while the caret could not move.
 */
internal fun deleteBackwards(
    text: String,
    selectionStart: Int,
    selectionEnd: Int,
): NumberEntry {
    val start = selectionStart.coerceIn(0, text.length)
    val end = selectionEnd.coerceIn(0, text.length)
    val from = minOf(start, end)
    val to = maxOf(start, end)
    if (from != to) return NumberEntry(text.replaceRange(from, to, ""), from)
    if (from == 0) return NumberEntry(text, 0)
    return NumberEntry(text.replaceRange(from - 1, from, ""), from - 1)
}
