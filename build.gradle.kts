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
            // koin-android:4.2.0 pulls androidx.activity-ktx 1.12.4, which needs AGP
            // 8.9.1+ (we're on 8.7.3). Force the whole androidx.activity family back to
            // the version we already build against; Koin only touches baseline
            // ComponentActivity/Application APIs present since well before 1.9.3.
            force(
                "androidx.activity:activity:1.9.3",
                "androidx.activity:activity-ktx:1.9.3",
                "androidx.activity:activity-compose:1.9.3",
            )
        }
    }
}
