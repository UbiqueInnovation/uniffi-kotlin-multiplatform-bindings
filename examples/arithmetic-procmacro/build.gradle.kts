import io.gitlab.trixnity.gradle.RustHost
import io.gitlab.trixnity.gradle.cargo.dsl.jvm
import io.gitlab.trixnity.gradle.cargo.dsl.linux
import io.gitlab.trixnity.gradle.cargo.dsl.posix
import io.gitlab.trixnity.gradle.rust.dsl.useRustUpLinker

plugins {
    kotlin("multiplatform")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform")
    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform")
    alias(libs.plugins.kotlin.atomicfu)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

uniffi {
    bindgenFromPath(rootProject.layout.projectDirectory.dir("bindgen"))
    generateFromLibrary()
}
cargo {
   builds.linux {
       usesCross = true
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
    if (RustHost.Platform.MacOS.isCurrent) {
        iosArm64()
        iosSimulatorArm64()
        iosX64()
        macosArm64()
        macosX64()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":runtime"))
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions.core)
            }
        }
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
        sourceCompatibility = JavaVersion.VERSION_22
        targetCompatibility = JavaVersion.VERSION_22
    }
}
