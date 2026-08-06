package org.carlospinan.bloqueador.app.calllog

import java.io.File

actual object RecordingStore {
    actual fun delete(path: String): Boolean {
        val file = File(path)
        // `File.delete()` returns false for a file that was never there, which is not a failure
        // for our purposes -- see the KDoc on the expect declaration.
        return !file.exists() || file.delete()
    }

    actual fun exists(path: String): Boolean = File(path).exists()
}
