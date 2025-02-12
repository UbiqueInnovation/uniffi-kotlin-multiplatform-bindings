import io.gitlab.trixnity.gradle.rust.dsl.hostNativeTarget
import io.gitlab.trixnity.gradle.rust.dsl.useRustUpLinker

plugins {
    kotlin("multiplatform")
    id("com.android.library")

    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform")
    id("io.gitlab.trixnity.rust.kotlin.multiplatform")
    alias(libs.plugins.kotlin.atomicfu)
}

uniffi {
    bindgenFromPath(layout.projectDirectory.dir("../bindgen-bootstrap"))

    generateFromLibrary()
}

kotlin {
    jvmToolchain(17)

    jvm()
    
    androidTarget()

    hostNativeTarget()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.jna)
        }

        commonMain.dependencies {
            implementation(libs.kotlinx.atomicfu)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.okio)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
        }

        jvmMain.dependencies {
            implementation(libs.jna)
        }

        nativeMain.dependencies {

        }
    }
}

android {
    namespace = "uniffi.runtime"
    compileSdk = 34
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}