plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ktlint) apply false
}

// Captured here: the `libs` accessor resolves in the root script's own scope, not inside the
// allprojects/configurations closure below.
val kotlinVersion = libs.versions.kotlin.get()

subprojects {
    // Generated code (SQLDelight, Compose Multiplatform resources) is excluded via the
    // root .editorconfig, not here -- ktlint-gradle's own `filter { exclude(...) }`
    // doesn't reliably reach generated dirs registered onto KMP source sets by other
    // plugins, but ktlint's EditorConfig handling (per file, inside its own engine) does.
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

allprojects {
    configurations.all {
        resolutionStrategy {
            // SQLDelight 2.3.2 depends on kotlin-stdlib 2.3.10, which drags the whole graph past
            // the 2.2.20 compiler we build with. Compiling against a newer stdlib than the
            // compiler is wrong on its own terms, and it breaks Android Lint outright: the UAST
            // frontend bundled with AGP 8.7.3 can't read Kotlin 2.3 metadata and fails every
            // lint task with "Module was compiled with an incompatible version of Kotlin".
            // Pinning to the toolchain's own version is the fix; raise both together.
            force(
                "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion",
                "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion",
            )
        }
    }
}
