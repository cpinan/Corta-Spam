package org.carlospinan.bloqueador.app.autoresponder

/**
 * Defaults the platform supplies for a greeting that has never been edited.
 *
 * The default script used to be an English constant in [AutoResponderConfig], so every user in
 * every locale got a greeting their caller might not understand — read aloud by a text-to-speech
 * engine set to the *device's* language, which made it worse. Android supplies a localized
 * string; iOS falls back to the constant while that target is parked.
 */
data class AutoResponderDefaults(
    val script: String = AutoResponderConfig.DEFAULT_SCRIPT,
)
