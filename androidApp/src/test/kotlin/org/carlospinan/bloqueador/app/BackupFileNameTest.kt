package org.carlospinan.bloqueador.app

import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupFileNameTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `name is a json file carrying the export time`() {
        // 2026-10-09T13:45:12Z
        assertEquals("cortaspam-backup-20261009-134512.json", backupFileName(1791553512000L, utc))
    }

    @Test
    fun `stamp follows the given time zone`() {
        val lima = TimeZone.getTimeZone("America/Lima")
        assertEquals("cortaspam-backup-20261009-084512.json", backupFileName(1791553512000L, lima))
    }

    @Test
    fun `two exports a second apart get different names`() {
        assert(backupFileName(1791553512000L, utc) != backupFileName(1791553513000L, utc))
    }
}
