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
        if (recordingEnabled && CONSENT_MARKERS.none { script.contains(it, ignoreCase = true) }) {
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

        /**
         * Phrases that satisfy the recording-consent gate, one per shipped locale.
         *
         * The check used to be against the English sentence alone, so a user writing their
         * greeting in Spanish, Hindi or Portuguese could never turn recording on — the only way
         * through was to paste an English sentence into a greeting the caller would hear in
         * another language. All four are accepted regardless of the device's current locale, so
         * changing language doesn't invalidate a greeting that was already lawful.
         */
        val CONSENT_MARKERS =
            listOf(
                "This call may be recorded",
                "Esta llamada puede ser grabada",
                "इस कॉल को रिकॉर्ड किया जा सकता है",
                "Esta chamada pode ser gravada",
            )

        /** The phrase shown to the user as the one to include; locale-specific rendering lives in the UI. */
        val CONSENT_MARKER: String get() = CONSENT_MARKERS.first()

        /**
         * Last-resort greeting when no platform-supplied default is available (iOS today).
         * Android injects a localized one — see `AutoResponderDefaults`.
         */
        const val DEFAULT_SCRIPT =
            "Hello. The person you are calling is not available. Please try again later."
    }
}
