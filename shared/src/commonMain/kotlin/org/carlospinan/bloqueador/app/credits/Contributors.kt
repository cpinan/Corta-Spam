package org.carlospinan.bloqueador.app.credits

/**
 * Someone who helped the app along — code, translations, bug reports, or the feedback that
 * changed a decision.
 *
 * [contribution] is free text and optional; when it is absent the screen shows the name alone.
 * It is *not* localized on purpose. Translating "reported the ringtone bug" per locale would mean
 * a resource key per person and a build failure every time someone is added without all four —
 * and a credit is a factual attribution, not app copy.
 */
data class Contributor(
    val name: String,
    val contribution: String? = null,
)

/**
 * The people the Credits screen lists, in the order they are shown.
 *
 * Empty on purpose right now: the screen exists and is reachable, and it tells the user that
 * names are on the way rather than pretending nobody helped. Add entries here and nothing else
 * has to change — the screen renders whatever this holds.
 */
val CONTRIBUTORS: List<Contributor> = emptyList()
