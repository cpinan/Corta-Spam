package org.carlospinan.bloqueador.app.autoresponder

/**
 * Why the recording switch being on will, or will not, produce a recording.
 *
 * The switch was a promise the app could not always keep: recording only ever happens on a call
 * the auto-responder itself answered, so with the auto-responder off — or with a greeting that
 * fails validation, which stops it answering anything — the switch sat on and nothing was ever
 * recorded, with nothing on screen saying why. Users read that as a broken feature, which is a
 * fair reading.
 *
 * What this cannot promise is [Ready] meaning a file appears. The microphone is the only audio
 * source a third-party app may use during a call, and several manufacturers hand it exclusively
 * to the telephony stack — see `AutoResponderRecorder`. That failure is only knowable on the
 * device, at the moment of the call, so the screen says so in prose rather than pretending to a
 * prediction here.
 */
enum class RecordingReadiness {
    /** Recording is switched off. Nothing to explain — the user asked for this. */
    Off,

    /** Everything this app can check is in place. */
    Ready,

    /** The auto-responder itself is off, so no call is ever answered to record. */
    AutoResponderOff,

    /** The greeting is rejected (empty, too long, or missing the consent sentence). */
    GreetingInvalid,

    /** `RECORD_AUDIO` has not been granted, so the recorder returns before it starts. */
    MicPermissionMissing,
}

/**
 * @param micPermissionGranted defaults to true so platforms with no such permission to check
 *   (iOS builds this screen too) don't report a blocker the user cannot clear.
 */
fun recordingReadiness(
    config: AutoResponderConfig,
    micPermissionGranted: Boolean = true,
): RecordingReadiness =
    when {
        !config.recordingEnabled -> RecordingReadiness.Off
        // Checked before the greeting: an off auto-responder is the bigger and simpler fact, and
        // reporting a greeting problem first sends the user to fix a field that changes nothing
        // until they find the switch above it.
        !config.enabled -> RecordingReadiness.AutoResponderOff
        config.validate() !is AutoResponderConfig.ValidationResult.Ok -> RecordingReadiness.GreetingInvalid
        !micPermissionGranted -> RecordingReadiness.MicPermissionMissing
        else -> RecordingReadiness.Ready
    }
