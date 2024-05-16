import io.gitlab.trixnity.gradle.CargoHost
import io.gitlab.trixnity.gradle.rustlink.useRustUpLinker

plugins {
    kotlin("multiplatform")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform")
    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform")
    alias(libs.plugins.kotlin.atomicfu)
    alias(libs.plugins.android.library)
}

uniffi {
    bindgenCratePath = rootProject.layout.projectDirectory.dir("bindgen")
    generateFromUdl {
        udlFile = layout.projectDirectory.file("src/todolist.udl")
        namespace = "todolist"
    }
}

kotlin {
    androidTarget()
    jvm("desktop")
    arrayOf(
        mingwX64(),
    ).forEach { nativeTarget ->
        nativeTarget.compilations.getByName("test") {
            useRustUpLinker()
        }
    }

    linuxX64()
    linuxArm64()
    if (CargoHost.Platform.MacOS.isCurrent) {
        iosArm64()
        iosSimulatorArm64()
        iosX64()
        macosArm64()
        macosX64()
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions.core)
            }
        }
    }
}

android {
    namespace = "io.gitlab.trixnity.uniffi.examples.todolist"
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
