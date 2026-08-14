import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

// Signing material is read from a properties file that is NOT in the repository (see
// .gitignore). Absent, the release build still assembles -- debug-signed, which is fine for
// verifying R8 locally and in CI, and rejected by Play, which is the correct failure.
val keystorePropertiesFile = rootProject.file("androidApp/keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }
val hasUploadKey = keystoreProperties.getProperty("storeFile") != null

// Single source of truth for the version, so the artifact filename and defaultConfig can never
// disagree. Uploading the wrong build is easy when every bundle is called androidApp-release.aab.
// Version codes 1, 2 and 3 are all spent, and Play will not accept a re-upload of a code it has
// already seen. A code is spent on upload, not on publication. 1 was submitted and rejected under
// the Full-Screen Intent policy. 2 was uploaded and never sent for review: it warned that 6
// previously supported devices had been dropped, because declaring RECORD_AUDIO makes Play imply
// android.hardware.microphone as required; the manifest now declares that feature optional. 3 was
// submitted to production and rejected under the same Full-Screen Intent policy as 1, with the
// declaration form already filed -- resolved by answering No to the form's pre-grant question
// rather than by dropping the permission, which means every Android 14+ install now starts with
// the app-op denied and the in-app grant route is load-bearing. 4 carries the onboarding
// checklist row that exposes that route. versionName stays 0.1.0 because nothing has ever reached
// a user under that name -- only the code had to move.
val appVersionName = "1.2.0"
val appVersionCode = 4

// Names the outputs corta-spam-<versionName>-<versionCode>-<buildType>.{aab,apk} instead of
// androidApp-release.*, so a bundle sitting in Downloads still says which release it is.
base {
    archivesName = "corta-spam-$appVersionName-$appVersionCode"
}

android {
    namespace = "org.carlospinan.bloqueador.app"
    compileSdk = 36

    defaultConfig {
        // The Play-facing identity, and permanent after the first upload -- it appears in
        // every store link. Deliberately different from `namespace` above, which stays
        // org.carlospinan.bloqueador.app: namespace drives the R class and resolves relative
        // manifest component names, so changing it would move every source package for no
        // user-visible gain. applicationId is the one users can actually see.
        applicationId = "org.carlospinan.cortaspam"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        // Only registered when the properties file is present, so a clean checkout still builds.
        if (hasUploadKey) {
            create("upload") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // The module had no buildTypes block at all, so `release` was AGP's default:
            // unminified, unshrunk, and debug-signed. Nothing about that build was shippable.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed only when a keystore.properties exists. Deliberately not an error when it
            // doesn't: CI has to be able to run assembleRelease to exercise R8 without holding
            // the signing key. Without it AGP emits androidApp-release-unsigned.apk, which Play
            // rejects -- the correct failure, and not the same as "debug-signed".
            signingConfig = if (hasUploadKey) signingConfigs.getByName("upload") else null
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
