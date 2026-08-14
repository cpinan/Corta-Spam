package org.carlospinan.bloqueador.app.calllog

/**
 * Formats a call-log timestamp in the reader's locale.
 *
 * This used to be built by hand in `commonMain`: `local.month.name` (an English enum constant),
 * truncated to three letters, with the day, year and a 24-hour clock pinned in that order. Every
 * string around it was translated, so Spanish, Portuguese and Hindi users read a fully localized
 * call log whose dates said "Aug 14, 2026" — found by screenshotting the app in Spanish, which no
 * unit test would have flagged because the English output was correct.
 *
 * There is no locale-aware date formatting in `kotlinx-datetime`, and hand-rolling one means 12
 * month names per locale plus each locale's field order and clock convention. Both platforms
 * already ship that knowledge, so this is an `expect`/`actual` over their formatters.
 */
expect fun formatCallTimestamp(epochMillis: Long): String
