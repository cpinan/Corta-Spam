plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
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
            implementation(libs.koin.core)
            // api, not implementation: consumers (androidApp) instantiate and hold
            // DialerOnboardingViewModel, so its ViewModel supertype must be resolvable
            // on their compile classpath too.
            api(libs.androidx.lifecycle.viewmodel)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
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

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("org.carlospinan.bloqueador.app.db")
        }
    }
}
