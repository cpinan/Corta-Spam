plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvmToolchain(17)

    androidTarget()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.navigation.compose)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.kotlinx.serialization.json)
            // api, not implementation: consumers (androidApp) instantiate and hold
            // DialerOnboardingViewModel, so its ViewModel supertype must be resolvable
            // on their compile classpath too.
            api(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.android)
        }
        val androidUnitTest by getting {
            dependencies {
                // Plain JDBC driver: works in a headless JVM unit test, unlike
                // AndroidSqliteDriver which needs a real/Robolectric Context.
                implementation(libs.sqldelight.sqlite.driver)
                implementation(libs.robolectric)
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.core)
                // Robolectric+Compose UI tests resolve their host ComponentActivity from this
                // manifest fragment. testImplementation (not debugImplementation) keeps it out
                // of androidApp's real debug APK -- see testOptions.unitTests below.
                implementation(libs.androidx.compose.ui.test.manifest)
            }
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

android {
    namespace = "org.carlospinan.bloqueador.app.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

compose.resources {
    // Pinned deliberately. Compose Multiplatform otherwise derives this package from
    // rootProject.name, so renaming the project silently breaks every `Res` import.
    packageOfResClass = "cortaspam.shared.generated.resources"
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("org.carlospinan.bloqueador.app.db")
            verifyMigrations.set(true)
            // Versioned schema snapshots. Without a baseline here, verifyMigrations replays
            // every .sqm on top of the *current* .sq and fails on already-applied columns.
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}
