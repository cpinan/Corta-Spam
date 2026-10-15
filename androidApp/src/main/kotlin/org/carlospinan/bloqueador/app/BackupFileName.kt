package org.carlospinan.bloqueador.app

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Suggested name for an exported backup: `cortaspam-backup-20261009-134512.json`.
 *
 * The extension matters as much as the stamp. Export used to share the JSON as `text/plain`, so it
 * landed on disk as `cortaspam.txt`, which the import picker (JSON only) then refused to list --
 * the app could not read its own backup. Timestamped so a second export never overwrites or
 * hides behind the first.
 */
internal fun backupFileName(
    epochMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): String {
    val stamp =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT)
            .apply { this.timeZone = timeZone }
            .format(Date(epochMillis))
    return "cortaspam-backup-$stamp.json"
}
