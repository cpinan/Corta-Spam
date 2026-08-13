package org.carlospinan.bloqueador.app.keypad

/**
 * A number handed to the app from outside — an `ACTION_DIAL` intent, or a "call back" tap on a
 * notification — to be shown on the keypad rather than dialled.
 *
 * [id] exists because the number alone is not enough to identify a *request*: tapping the same
 * `tel:` link twice must re-open the keypad the second time too, and a plain `String?` compared by
 * value cannot tell "again" from "unchanged". It is also what lets the keypad recognise a request
 * it has already applied, so returning to the tab later does not resurrect a number the user
 * deleted.
 */
data class DialRequest(
    val number: String,
    val id: Long,
)
