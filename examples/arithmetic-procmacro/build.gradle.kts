import io.gitlab.trixnity.gradle.CargoHost

plugins {
    kotlin("multiplatform")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform")
    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform")
    alias(libs.plugins.kotlin.atomicfu)
    alias(libs.plugins.android.library)
}

uniffi {
    bindgenCratePath = rootProject.layout.projectDirectory.dir("bindgen")
    generateFromLibrary()
}

kotlin {
    androidTarget()
    jvm("desktop")
    mingwX64()
    linuxX64()
    linuxArm64()
    if (CargoHost.Platform.MacOS.isCurrent) {
        iosArm64()
        iosSimulatorArm64()
        iosX64()
        macosArm64()
        macosX64()
    }
}

android {
    namespace = "io.gitlab.trixnity.uniffi.examples.arithmeticpm"
    compileSdk = 34

    defaultConfig {
        consumerProguardFiles("proguard-rules.pro")
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
}
