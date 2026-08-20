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
 * A third-party library the shipped app is built out of.
 *
 * Listed separately from [Contributor] because the obligation is different: a person is credited
 * as a courtesy, a library under Apache-2.0 or MIT is credited because its licence asks to be.
 * [license] is the SPDX identifier and [url] the project's own home, so a reader can check the
 * terms rather than take this screen's word for them.
 *
 * Test-only and build-only dependencies are deliberately absent — nothing of Robolectric or the
 * Gradle plugins reaches a user's device, so crediting them here would misstate what is running.
 */
data class OpenSourceComponent(
    val name: String,
    val license: String,
    val url: String,
)

/**
 * The people the Credits screen lists, in the order they are shown.
 *
 * Add an entry here and nothing else has to change — the screen renders whatever this holds.
 */
val CONTRIBUTORS: List<Contributor> =
    listOf(
        Contributor(
            name = "Carlos Eduardo Piñán Indacochea",
            contribution = "Author and maintainer",
        ),
        Contributor(
            name = "Claude (Anthropic)",
            contribution = "AI pair programmer — code, tests and documentation, reviewed and merged by the maintainer",
        ),
        // The people who used the app before it was good. Listed after the maintainer and in the
        // order the maintainer named them, not alphabetically -- this is an acknowledgement, and
        // re-sorting someone's thanks is not an improvement.
        Contributor(
            name = "Sig Mandel",
            contribution = "Early feedback, bug reporting and testing",
        ),
        Contributor(
            name = "Faride Altamirano",
            contribution = "My wife — early feedback, bug reporting and testing",
        ),
        Contributor(
            name = "Jose Arellano",
            contribution = "Early feedback, bug reporting and testing",
        ),
        Contributor(
            name = "Augusto Piñán",
            contribution = "Early feedback, bug reporting and testing",
        ),
    )

/**
 * The open-source libraries the app ships with, grouped the way a reader would look for them
 * rather than the way the dependency graph resolves them: the whole Kotlin/Compose stack is one
 * project to the person reading, not eleven Maven coordinates.
 */
val OPEN_SOURCE_COMPONENTS: List<OpenSourceComponent> =
    listOf(
        OpenSourceComponent(
            name = "Kotlin and Kotlin Multiplatform",
            license = "Apache-2.0",
            url = "https://github.com/JetBrains/kotlin",
        ),
        OpenSourceComponent(
            name = "Compose Multiplatform",
            license = "Apache-2.0",
            url = "https://github.com/JetBrains/compose-multiplatform",
        ),
        OpenSourceComponent(
            name = "Jetpack Compose and AndroidX",
            license = "Apache-2.0",
            url = "https://github.com/androidx/androidx",
        ),
        OpenSourceComponent(
            name = "kotlinx.coroutines, datetime and serialization",
            license = "Apache-2.0",
            url = "https://github.com/Kotlin",
        ),
        OpenSourceComponent(
            name = "Koin",
            license = "Apache-2.0",
            url = "https://github.com/InsertKoinIO/koin",
        ),
        OpenSourceComponent(
            name = "SQLDelight",
            license = "Apache-2.0",
            url = "https://github.com/sqldelight/sqldelight",
        ),
    )
