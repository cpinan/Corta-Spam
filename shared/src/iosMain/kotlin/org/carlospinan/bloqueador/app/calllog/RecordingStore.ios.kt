package org.carlospinan.bloqueador.app.calllog

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

/**
 * iOS never writes a recording (no auto-responder there), so in practice these paths never
 * exist. Implemented against the real filesystem anyway rather than stubbed to `true`: a stub
 * would make "clear log" claim it deleted audio it never looked at, and if a recording ever does
 * arrive on iOS via a restored backup this behaves correctly instead of silently lying.
 */
actual object RecordingStore {
    // removeItemAtPath's out-error parameter is a CPointer, which is behind the foreign-API
    // opt-in. Null is passed deliberately: the Boolean return already says whether the file is
    // gone, which is all the caller acts on.
    @OptIn(ExperimentalForeignApi::class)
    actual fun delete(path: String): Boolean {
        val manager = NSFileManager.defaultManager
        if (!manager.fileExistsAtPath(path)) return true
        return manager.removeItemAtPath(path, null)
    }

    actual fun exists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)
}
