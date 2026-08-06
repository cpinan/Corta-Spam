package org.carlospinan.bloqueador.app.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

/**
 * Records the caller's message after the auto-responder greeting, into app-internal storage.
 *
 * **This records the microphone, not the call stream.** Android reserves the real call audio
 * sources (`VOICE_CALL`, `VOICE_DOWNLINK`, `VOICE_UPLINK`) behind `CAPTURE_AUDIO_OUTPUT`, a
 * `signature|privileged` permission no third-party app can hold -- holding the dialer role does
 * not grant it either. What is left is [MediaRecorder.AudioSource.MIC], which picks up the
 * caller acoustically through the earpiece, the same coupling the greeting relies on in the
 * other direction.
 *
 * The consequence is that this is **best effort and genuinely fails on some devices**: several
 * OEMs (Motorola among them, per this project's own on-device findings) give the telephony
 * stack exclusive ownership of the mic during a call as an anti-wiretapping measure, so
 * [start] returns false and no file is produced. Callers must treat a false return as normal,
 * not as an error worth surfacing as a crash.
 *
 * Files live in `filesDir/recordings/`, which is app-private and excluded from cloud backup
 * along with the rest of the app's data.
 */
class AutoResponderRecorder(
    private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    /**
     * Begins recording for call-log entry [entryId].
     *
     * @return true when the microphone was actually acquired. False covers a missing
     *   `RECORD_AUDIO` grant and a mic the telephony stack refuses to share; both are ordinary
     *   outcomes, and in both cases nothing is left on disk.
     */
    fun start(entryId: Long): Boolean {
        if (!hasPermission()) {
            Log.i(TAG, "RECORD_AUDIO not granted; skipping recording")
            return false
        }
        if (recorder != null) {
            Log.w(TAG, "Recorder already running; refusing to start a second one")
            return false
        }

        val file = File(recordingsDir(), "call_$entryId.m4a")
        return try {
            val created =
                newRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setMaxDuration(MAX_DURATION_MILLIS)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
            recorder = created
            outputFile = file
            true
        } catch (e: IOException) {
            // prepare() could not open the output file.
            Log.w(TAG, "Could not prepare the recorder", e)
            cleanUpAfterFailure(file)
            false
        } catch (e: IllegalStateException) {
            // start() on a mic the platform would not hand over -- the common OEM case.
            Log.w(TAG, "Microphone unavailable during the call", e)
            cleanUpAfterFailure(file)
            false
        } catch (e: RuntimeException) {
            // MediaRecorder.start() throws a bare RuntimeException for several device-specific
            // failures. A live call is in progress; nothing here is worth taking the service down.
            Log.w(TAG, "Recorder failed to start", e)
            cleanUpAfterFailure(file)
            false
        }
    }

    /**
     * Stops recording and returns the finished file's path, or null when nothing usable exists.
     *
     * `MediaRecorder.stop()` throws when it is stopped before capturing any frames, which
     * leaves a zero-byte file behind; that case returns null and deletes the file rather than
     * handing back a path to silence.
     */
    fun stop(): String? {
        val active = recorder ?: return null
        val file = outputFile
        recorder = null
        outputFile = null

        return try {
            active.stop()
            active.release()
            if (file != null && file.length() > 0) {
                file.absolutePath
            } else {
                file?.delete()
                null
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "Recorder stopped before capturing anything", e)
            active.release()
            file?.delete()
            null
        }
    }

    /** Abandons an in-flight recording and deletes the partial file. */
    fun cancel() {
        val active = recorder ?: return
        val file = outputFile
        recorder = null
        outputFile = null
        try {
            active.stop()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Recorder was not running when cancelled", e)
        }
        active.release()
        file?.delete()
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun newRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun recordingsDir(): File = File(context.filesDir, RECORDINGS_DIR).apply { mkdirs() }

    private fun cleanUpAfterFailure(file: File) {
        recorder?.release()
        recorder = null
        outputFile = null
        file.delete()
    }

    companion object {
        private const val TAG = "AutoResponderRecorder"
        const val RECORDINGS_DIR = "recordings"

        /**
         * Hard cap on one recording. A blocked caller who never hangs up would otherwise record
         * until the call does, and these files are never pruned automatically -- only the user's
         * own delete or "clear log" removes them.
         */
        const val MAX_DURATION_MILLIS = 60_000
    }
}
