package org.carlospinan.bloqueador.app.telecom

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File

/**
 * Plays back a saved auto-responder recording inside the app.
 *
 * Separate from [AutoResponderAudio], which routes with `USAGE_VOICE_COMMUNICATION` because it
 * is speaking into a live call. This is ordinary media playback for the user, so it uses
 * `USAGE_MEDIA` and follows the media volume rather than the in-call volume.
 */
class RecordingPlayer {
    private var player: MediaPlayer? = null

    /** Plays [path], replacing whatever was already playing. No-op when the file is gone. */
    fun play(path: String) {
        stop()
        if (!File(path).exists()) {
            // The row can outlive the file if storage was cleared externally. Nothing to play
            // and nothing worth crashing over.
            Log.w(TAG, "Recording file no longer exists")
            return
        }
        try {
            player =
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    setDataSource(path)
                    setOnCompletionListener { stop() }
                    setOnErrorListener { _, _, _ ->
                        stop()
                        true
                    }
                    setOnPreparedListener { start() }
                    prepareAsync()
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not play the recording", e)
            stop()
        }
    }

    fun stop() {
        player?.runCatching { release() }
        player = null
    }

    private companion object {
        const val TAG = "RecordingPlayer"
    }
}
