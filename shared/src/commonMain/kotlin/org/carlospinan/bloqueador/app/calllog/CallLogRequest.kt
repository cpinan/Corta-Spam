package org.carlospinan.bloqueador.app.calllog

/**
 * "Open the call log on this caller" -- what tapping a blocked, missed or repeat-caller
 * notification asks the app to do.
 *
 * Carries an [id] for the same reason `DialRequest` does: two taps on the same caller's
 * notification are two requests, and comparing by number alone would make the second one a
 * no-op because nothing about the value changed.
 */
data class CallLogRequest(
    val number: String,
    val id: Long,
)
