package org.carlospinan.bloqueador.app.autoresponder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutoResponderConfigTest {
    @Test
    fun disabled_alwaysValid() {
        val config = AutoResponderConfig(enabled = false, script = "")
        assertTrue(config.validate() is AutoResponderConfig.ValidationResult.Ok)
    }

    @Test
    fun enabled_emptyScriptAndNoAudio_error() {
        val config = AutoResponderConfig(enabled = true, script = "", audioUri = "")
        val result = config.validate()
        assertTrue(result is AutoResponderConfig.ValidationResult.Error)
        assertEquals(
            AutoResponderConfig.ErrorCode.EMPTY_SCRIPT,
            (result as AutoResponderConfig.ValidationResult.Error).code,
        )
    }

    @Test
    fun enabled_withScript_ok() {
        val config = AutoResponderConfig(enabled = true, script = "Hello")
        assertTrue(config.validate() is AutoResponderConfig.ValidationResult.Ok)
    }

    @Test
    fun enabled_withAudioOnly_ok() {
        val config = AutoResponderConfig(enabled = true, script = "", audioUri = "content://audio/1")
        assertTrue(config.validate() is AutoResponderConfig.ValidationResult.Ok)
    }

    @Test
    fun scriptTooLong_error() {
        val config =
            AutoResponderConfig(
                enabled = true,
                script = "x".repeat(AutoResponderConfig.MAX_SCRIPT_LENGTH + 1),
            )
        val result = config.validate()
        assertTrue(result is AutoResponderConfig.ValidationResult.Error)
        assertEquals(
            AutoResponderConfig.ErrorCode.SCRIPT_TOO_LONG,
            (result as AutoResponderConfig.ValidationResult.Error).code,
        )
    }

    @Test
    fun recordingWithoutConsent_error() {
        val config =
            AutoResponderConfig(
                enabled = true,
                script = "Hello, leave a message.",
                recordingEnabled = true,
            )
        val result = config.validate()
        assertTrue(result is AutoResponderConfig.ValidationResult.Error)
        assertEquals(
            AutoResponderConfig.ErrorCode.MISSING_CONSENT,
            (result as AutoResponderConfig.ValidationResult.Error).code,
        )
    }

    @Test
    fun recordingWithConsent_ok() {
        val config =
            AutoResponderConfig(
                enabled = true,
                script = "Hello. This call may be recorded. Leave a message.",
                recordingEnabled = true,
            )
        assertTrue(config.validate() is AutoResponderConfig.ValidationResult.Ok)
    }

    @Test
    fun usesTts_whenAudioUriBlank() {
        assertTrue(AutoResponderConfig(audioUri = "").usesTts)
        assertTrue(!AutoResponderConfig(audioUri = "content://x").usesTts)
    }
}
