package org.carlospinan.bloqueador.app.autoresponder

/**
 * Whether a blocked call should be auto-answered for the greeting, as opposed to rejected
 * outright.
 *
 * Extracted from `PassthroughInCallService` (androidApp) so the gate itself — the thing a
 * disabled or misconfigured auto-responder most often gets blamed for — can be unit tested
 * without a Telecom stack.
 */
object AutoResponderTriggerPolicy {
    fun shouldGreet(
        config: AutoResponderConfig,
        hasOtherLiveCall: Boolean,
    ): Boolean =
        config.enabled &&
            config.validate() is AutoResponderConfig.ValidationResult.Ok &&
            // Answering forces the loudspeaker and opens the microphone, and both of those are
            // properties of the audio session rather than of one call: greeting a blocked caller
            // while the user is mid-conversation would move *their* call onto the speaker and put
            // the greeting down its uplink. A blocked call arriving during a real one is simply
            // rejected.
            !hasOtherLiveCall
}
