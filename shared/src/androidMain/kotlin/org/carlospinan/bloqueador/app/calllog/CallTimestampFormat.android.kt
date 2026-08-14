package org.carlospinan.bloqueador.app.calllog

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * `Locale.getDefault()` is read on every call rather than cached in a formatter: the app supports
 * per-app locales, so a user switching Corta Spam to Spanish from Android's app-language settings
 * changes it inside a running process, and a formatter built once at class-init would keep
 * printing the language they just left.
 */
actual fun formatCallTimestamp(epochMillis: Long): String =
    DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMillis))
