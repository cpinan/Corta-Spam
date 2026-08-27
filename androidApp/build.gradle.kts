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
// 5, because 4 was uploaded to the internal track on 2026-08-13 and a code is spent on upload
// rather than on publication -- Play will not take it twice. 1.3.0 rather than 1.2.1 because this
// is not a patch on 1.2.0: it adds contact search, call-log filters, outgoing calls in the log and
// caller identity on the call screen, and it fixes a crash that killed the app on every answered
// call, which made Telecom hand the live call to the preloaded dialer mid-conversation.
// 7, and 1.5.0 rather than 1.4.1, because the 1.4.0 (6) artifact is being retired rather than
// shipped. It was built on 2026-08-14 and uploaded -- Play attaches its quality advisories to
// "Release name: 6 (1.4.0)", which is how we know -- so code 6 is spent and could not be reused
// even if we wanted to. Twenty-one commits landed after that upload, so the binary carrying that
// version string no longer describes what is in the tree.
//
// A minor bump rather than a patch because the batch is not bug fixes: dark mode, mute, speaker and
// a call timer on the in-call screen, a DTMF keypad, saving a typed number as a contact, the
// emergency-callback exemption, and the proximity blank-screen all arrived after 1.4.0 was cut.
//
// The reason 6 is not simply reused: it can only ever be uploaded once, and the build that would
// carry it is not the build that was tested as 1.4.0. Two different binaries answering to one
// version is the thing this naming scheme exists to prevent -- and it had already happened locally,
// with a rebuilt corta-spam-1.4.0-6-release.apk sitting next to the .aab of the same name from four
// days earlier.
//
// 1.5.0 also carries the fix that makes the previous artifact unshippable: holding ROLE_DIALER, it
// could not dial an emergency number. See docs/store/RELEASE_NOTES_1.5.0.md.
//
// 1.6.0 was first built on code 7, on this file's own claim that 7 had never been uploaded. Play
// rejected it: "Version code 7 has already been used." So 7 is spent, and the record here was
// wrong in exactly the way it was wrong about 6 four days earlier -- a code is spent on UPLOAD,
// and nothing local can see an upload. The only reliable evidence is the Console.
//
// 8, therefore, with the name left at 1.6.0: the app did not change between the two builds, only
// the number it answers to. The 1.6.0 (7) bundle is in superseded/ beside 1.5.0 (7), because it
// can never be uploaded and two binaries answering to one version string is what this naming
// scheme exists to prevent.
//
// The lesson this file keeps re-learning, now written where the number lives: do not record a
// code as unspent. Record it as unknown until the Console says otherwise.
//
// 9, and 1.6.1 rather than 1.7.0, because this release is one bug report answered: a blocked call
// that something else answered first stayed connected while the notification said it had been
// blocked, and an auto-responder greeting that never played held the call open until the caller
// gave up. No feature was added -- the confirmation dialog in front of the auto-responder switch
// exists because the feature surprised the person who turned it on, not because it does anything
// new.
//
// 9 was confirmed free against the Console on 2026-08-27 -- 8 (1.6.0) had been live at 100% since
// 2026-08-21 with nothing newer -- and then **uploaded the same day, which spends it**. The next
// build takes 10 or higher, and 9 can never be uploaded again whatever happens to this bundle in
// review.
//
// That sequence is the one to copy: read the Console, build, upload, and write the upload down in
// the same breath. Codes 6, 7 and 8 were each recorded in this file as unspent and each turned out
// to have been uploaded, because a code is spent on upload and an upload leaves no trace on this
// machine. The comments above are the scar tissue. Read the Console again before the next bump; do
// not read this line.
//
// See docs/store/RELEASE_NOTES_1.6.1.md.
val appVersionName = "1.6.1"
val appVersionCode = 9

// Names the outputs <versionCode>-<versionName>-<buildType>.{aab,apk} instead of
// androidApp-release.*, so a bundle sitting in Downloads still says which release it is.
//
// The version *code* leads, because it is the field an upload is accepted or rejected on and the
// one that may only ever increase -- a folder of these sorts into upload order by name alone.
// Earlier artifacts carry the older corta-spam-<name>-<code> form; they are left as they were,
// since renaming them would falsify the record of what was built under which filename.
base {
    archivesName = "$appVersionCode-$appVersionName"
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
