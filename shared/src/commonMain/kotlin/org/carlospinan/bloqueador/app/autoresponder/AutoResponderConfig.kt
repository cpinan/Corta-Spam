package org.carlospinan.bloqueador.app.autoresponder

/** Auto-responder configuration. Recording is a separate toggle (off by default). */
data class AutoResponderConfig(
    val enabled: Boolean = false,
    val script: String = DEFAULT_SCRIPT,
    val audioUri: String = "",
    val recordingEnabled: Boolean = false,
) {
    val usesTts: Boolean get() = audioUri.isBlank()

    fun validate(): ValidationResult {
        if (!enabled) return ValidationResult.Ok
        if (script.isBlank() && audioUri.isBlank()) {
            return ValidationResult.Error(ErrorCode.EMPTY_SCRIPT)
        }
        if (script.length > MAX_SCRIPT_LENGTH) {
            return ValidationResult.Error(ErrorCode.SCRIPT_TOO_LONG)
        }
        if (recordingEnabled && !script.contains(CONSENT_MARKER, ignoreCase = true)) {
            return ValidationResult.Error(ErrorCode.MISSING_CONSENT)
        }
        return ValidationResult.Ok
    }

    sealed class ValidationResult {
        data object Ok : ValidationResult()

        data class Error(
            val code: ErrorCode,
        ) : ValidationResult()
    }

    enum class ErrorCode {
        EMPTY_SCRIPT,
        SCRIPT_TOO_LONG,
        MISSING_CONSENT,
    }

    companion object {
        const val MAX_SCRIPT_LENGTH = 500
        const val CONSENT_MARKER = "This call may be recorded"
        const val DEFAULT_SCRIPT =
            "Hello. The person you are calling is not available. Please try again later."
    }
}
