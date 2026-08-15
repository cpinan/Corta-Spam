package org.carlospinan.bloqueador.app.autoresponder

import kotlin.test.Test
import kotlin.test.assertEquals

private const val CONSENTING_SCRIPT = "Hello. This call may be recorded. Please leave a message."

class RecordingReadinessTest {
    @Test
    fun recordingOffNeedsNoExplanation() {
        val config = AutoResponderConfig(enabled = true, script = CONSENTING_SCRIPT, recordingEnabled = false)

        assertEquals(RecordingReadiness.Off, recordingReadiness(config, micPermissionGranted = true))
    }

    /**
     * The reported "recording is not working". Recording only ever runs on a call the
     * auto-responder itself answered, so with the responder off the switch is inert — and the
     * screen said nothing about it.
     */
    @Test
    fun recordingCannotRunWhileTheAutoResponderIsOff() {
        val config = AutoResponderConfig(enabled = false, script = CONSENTING_SCRIPT, recordingEnabled = true)

        assertEquals(RecordingReadiness.AutoResponderOff, recordingReadiness(config, micPermissionGranted = true))
    }

    /** An invalid greeting stops the responder answering at all, so recording never starts either. */
    @Test
    fun recordingCannotRunWithAGreetingThatFailsValidation() {
        val config = AutoResponderConfig(enabled = true, script = "No consent sentence here.", recordingEnabled = true)

        assertEquals(RecordingReadiness.GreetingInvalid, recordingReadiness(config, micPermissionGranted = true))
    }

    @Test
    fun recordingCannotRunWithoutTheMicrophonePermission() {
        val config = AutoResponderConfig(enabled = true, script = CONSENTING_SCRIPT, recordingEnabled = true)

        assertEquals(RecordingReadiness.MicPermissionMissing, recordingReadiness(config, micPermissionGranted = false))
    }

    @Test
    fun everythingInPlaceReportsReady() {
        val config = AutoResponderConfig(enabled = true, script = CONSENTING_SCRIPT, recordingEnabled = true)

        assertEquals(RecordingReadiness.Ready, recordingReadiness(config, micPermissionGranted = true))
    }

    /**
     * An off auto-responder is reported ahead of a bad greeting: it is the bigger fact and the
     * one switch above the field, and naming the greeting first sends the user to edit text that
     * changes nothing until they find the switch.
     */
    @Test
    fun theAutoResponderSwitchIsReportedBeforeTheGreeting() {
        val config = AutoResponderConfig(enabled = false, script = "", recordingEnabled = true)

        assertEquals(RecordingReadiness.AutoResponderOff, recordingReadiness(config, micPermissionGranted = false))
    }

    /** iOS builds this screen with no permission to check, so it must not invent a blocker. */
    @Test
    fun aPlatformWithNoMicrophonePermissionToCheckIsReady() {
        val config = AutoResponderConfig(enabled = true, script = CONSENTING_SCRIPT, recordingEnabled = true)

        assertEquals(RecordingReadiness.Ready, recordingReadiness(config))
    }

    /** The consent gate accepts the sentence in any shipped locale, so readiness must too. */
    @Test
    fun aSpanishConsentSentenceIsAcceptedAsAValidGreeting() {
        val config =
            AutoResponderConfig(
                enabled = true,
                script = "Hola. Esta llamada puede ser grabada. Deja tu mensaje.",
                recordingEnabled = true,
            )

        assertEquals(RecordingReadiness.Ready, recordingReadiness(config, micPermissionGranted = true))
    }
}
