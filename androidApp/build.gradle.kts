plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "org.carlospinan.bloqueador.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.carlospinan.bloqueador.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // The module had no buildTypes block at all, so `release` was AGP's default:
            // unminified, unshrunk, and debug-signed. Nothing about that build was shippable.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // No signingConfig here on purpose: the upload key is not in this repository. Wire
            // one in from a local keystore or the CI secret store before publishing.
        }
        debug {
            // R8 changes behaviour (reflection, serialization, Telecom callbacks); leaving it off
            // for debug is fine, but the release rules still have to be exercised somewhere --
            // `assembleRelease` in CI is what does that.
            isMinifyEnabled = false
        }
    }

    lint {
        // Lint findings should fail the build in CI rather than accumulate unread in a report.
        warningsAsErrors = false
        abortOnError = true
        // Translation completeness is checked by TranslationCompletenessTest, which covers both
        // resource systems; Lint only sees the Android one and would report false gaps.
        disable += "MissingTranslation"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Robolectric needs the merged resources/manifest to build a real Context.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.datetime)
    // Used directly by CortaSpamApp and PassthroughInCallService; was only resolving
    // transitively, which a dependency bump elsewhere could quietly take away.
    implementation(libs.kotlinx.coroutines.core)

    // This module had no test source set at all until 2026-08-05. Its Android-only classes
    // (telecom bridge, notifications, contact lookup) need a Context, so they run under
    // Robolectric on the JVM -- same setup the :shared androidUnitTest source set uses.
    testImplementation(kotlin("test"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
}
