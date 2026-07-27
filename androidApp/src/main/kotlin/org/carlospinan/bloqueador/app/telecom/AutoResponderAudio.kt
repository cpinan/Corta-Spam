package org.carlospinan.bloqueador.app.telecom

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Plays a greeting via TTS or pre-recorded audio after a blocked call is answered.
 * Audio goes to the device speaker/earpiece (caller hears via acoustic coupling).
 */
class AutoResponderAudio(
    private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private val ttsReady = AtomicBoolean(false)
    private var pendingSpeak: Pair<String, () -> Unit>? = null

    fun prepare() {
        if (tts != null) return
        tts =
            TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    tts?.setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    ttsReady.set(true)
                    pendingSpeak?.let { (text, onDone) ->
                        pendingSpeak = null
                        speakInternal(text, onDone)
                    }
                }
            }
    }

    fun play(
        script: String,
        audioUri: String?,
        onComplete: () -> Unit,
    ) {
        if (!audioUri.isNullOrBlank()) {
            playFile(audioUri, onComplete)
        } else {
            speak(script, onComplete)
        }
    }

    private fun speak(
        text: String,
        onComplete: () -> Unit,
    ) {
        if (ttsReady.get()) {
            speakInternal(text, onComplete)
        } else {
            pendingSpeak = text to onComplete
            prepare()
        }
    }

    private fun speakInternal(
        text: String,
        onComplete: () -> Unit,
    ) {
        val engine =
            tts ?: run {
                onComplete()
                return
            }
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    onComplete()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onComplete()
                }
            },
        )
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "autoresponder")
    }

    private fun playFile(
        uriString: String,
        onComplete: () -> Unit,
    ) {
        try {
            mediaPlayer?.release()
            mediaPlayer =
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    setDataSource(context, Uri.parse(uriString))
                    setOnCompletionListener {
                        release()
                        mediaPlayer = null
                        onComplete()
                    }
                    setOnErrorListener { _, _, _ ->
                        release()
                        mediaPlayer = null
                        onComplete()
                        true
                    }
                    prepare()
                    start()
                }
        } catch (_: Exception) {
            onComplete()
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady.set(false)
        pendingSpeak = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
