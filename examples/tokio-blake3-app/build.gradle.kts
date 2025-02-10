import io.gitlab.trixnity.gradle.RustHost

plugins {
    kotlin("multiplatform")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform")
    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform")
    alias(libs.plugins.kotlin.atomicfu)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

uniffi {
    bindgenFromPath(rootProject.layout.projectDirectory.dir("bindgen"))
    generateFromLibrary()
}

kotlin {
    androidTarget()

    if (RustHost.Platform.MacOS.isCurrent) {
        arrayOf(
            iosArm64(),
            iosSimulatorArm64(),
            iosX64(),
        ).forEach {
            it.binaries.framework {
                baseName = "TokioBlake3AppKotlin"
                isStatic = true
                binaryOption("bundleId", "io.gitlab.trixnity.uniffi.examples.tokioblake3app.kotlin")
                binaryOption("bundleVersion", "0")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace = "io.gitlab.trixnity.uniffi.examples.tokioblake3app"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.gitlab.trixnity.uniffi.examples.tokioblake3app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        ndk.abiFilters.add("arm64-v8a")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}
