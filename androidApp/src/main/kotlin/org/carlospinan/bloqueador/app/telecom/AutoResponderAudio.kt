package org.carlospinan.bloqueador.app.telecom

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.net.toUri
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Plays a greeting via TTS or pre-recorded audio after a blocked call is answered.
 * Audio goes to the device speaker/earpiece (caller hears via acoustic coupling).
 *
 * Every path through this class must end in exactly one `onComplete`, because the caller is
 * connected until it fires: [PassthroughInCallService] answers the call to play the greeting and
 * hangs up when it finishes. A path that silently reports nothing leaves a blocked call live on
 * the loudspeaker, which is what the user hears as the app answering a call by itself.
 */
class AutoResponderAudio(
    private val context: Context,
) {
    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private val ttsReady = AtomicBoolean(false)

    /**
     * A greeting queued while the engine was still binding.
     *
     * Carries both callbacks, not just completion: the caller times out a greeting that never
     * *starts*, and dropping [onStarted] here would leave a perfectly good greeting -- the common
     * case, since the call is answered while TTS is still initialising -- looking like one that
     * never began.
     */
    private class PendingGreeting(
        val text: String,
        val onStarted: () -> Unit,
        val onComplete: () -> Unit,
    )

    private var pendingSpeak: PendingGreeting? = null

    fun prepare() {
        if (tts != null) return
        tts =
            TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // The result is worth reading: an engine with no voice for the device's
                    // language still initialises successfully and then says nothing. Spanish,
                    // Portuguese and Hindi voices are all downloads on devices that ship with
                    // English only. The greeting is attempted anyway -- most engines fall back to
                    // their default voice -- but the utterance callbacks are what actually end the
                    // call, so a failure here must not be the thing that decides it.
                    val language = tts?.setLanguage(Locale.getDefault())
                    if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "No installed voice for ${Locale.getDefault()}; using the engine default")
                    }
                    tts?.setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    ttsReady.set(true)
                    pendingSpeak?.let { pending ->
                        pendingSpeak = null
                        speakInternal(pending.text, pending.onStarted, pending.onComplete)
                    }
                } else {
                    // The engine is not coming. It used to be left in place, unusable and
                    // non-null, so `prepare()` would never rebuild it and the greeting queued in
                    // `pendingSpeak` was dropped without ever reporting completion -- the call
                    // stayed answered until the caller gave up.
                    Log.w(TAG, "Text-to-speech failed to initialise (status $status)")
                    tts?.shutdown()
                    tts = null
                    ttsReady.set(false)
                    val pending = pendingSpeak
                    pendingSpeak = null
                    pending?.onComplete?.invoke()
                }
            }
    }

    /**
     * Plays the greeting, preferring the user's own recording and falling back to the script.
     *
     * The fallback is not decoration. A chosen audio file can stop being readable between the
     * moment it was picked and the call that needs it — the provider is uninstalled, the SD card
     * is out, the grant was never persistable — and the previous behaviour on any of those was to
     * report completion having said nothing at all, so the caller was answered and hung up on in
     * silence. The script is always available, so there is no reason to greet with nothing.
     *
     * [onStarted] fires when sound actually begins. It is separate from [onComplete] because the
     * two failures are different: a greeting that never starts is a broken engine and the call
     * should end now, while a greeting that started and never finished is a lost callback that
     * should be given its full length first. Both callbacks arrive on an engine or playback
     * thread, never the caller's.
     */
    fun play(
        script: String,
        audioUri: String?,
        onStarted: () -> Unit = {},
        onComplete: () -> Unit,
    ) {
        if (!audioUri.isNullOrBlank()) {
            playFile(audioUri, onStarted, onComplete, onFailure = { speak(script, onStarted, onComplete) })
        } else {
            speak(script, onStarted, onComplete)
        }
    }

    private fun speak(
        text: String,
        onStarted: () -> Unit,
        onComplete: () -> Unit,
    ) {
        if (ttsReady.get()) {
            speakInternal(text, onStarted, onComplete)
        } else {
            pendingSpeak = PendingGreeting(text, onStarted, onComplete)
            prepare()
        }
    }

    private fun speakInternal(
        text: String,
        onStarted: () -> Unit,
        onComplete: () -> Unit,
    ) {
        val engine =
            tts ?: run {
                onComplete()
                return
            }
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onStarted()
                }

                override fun onDone(utteranceId: String?) {
                    onComplete()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onComplete()
                }
            },
        )
        // `speak` refusing the utterance is a silent failure otherwise: it returns ERROR without
        // calling the listener at all, so nothing would ever report this greeting as finished.
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Text-to-speech refused the greeting")
            onComplete()
        }
    }

    private fun playFile(
        uriString: String,
        onStarted: () -> Unit,
        onComplete: () -> Unit,
        onFailure: () -> Unit,
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
                    setDataSource(context, uriString.toUri())
                    setOnPreparedListener {
                        start()
                        onStarted()
                    }
                    setOnCompletionListener {
                        release()
                        mediaPlayer = null
                        onComplete()
                    }
                    setOnErrorListener { _, _, _ ->
                        release()
                        mediaPlayer = null
                        onFailure()
                        true
                    }
                    prepareAsync()
                }
        } catch (e: Exception) {
            // SecurityException is the one that actually happens: a content URI whose read grant
            // did not outlive the activity that picked it. IOException covers a file that has
            // been deleted or moved since.
            Log.w(TAG, "Could not open the greeting audio; falling back to the script", e)
            mediaPlayer?.release()
            mediaPlayer = null
            onFailure()
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

    private companion object {
        const val TAG = "AutoResponderAudio"
        const val UTTERANCE_ID = "autoresponder"
    }
}
