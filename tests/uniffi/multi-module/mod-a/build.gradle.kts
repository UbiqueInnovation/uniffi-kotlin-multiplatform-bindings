plugins {
    kotlin("multiplatform")
    id("com.android.library")

    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform")
    alias(libs.plugins.kotlin.atomicfu)
}


uniffi {
    bindgenFromPath(layout.projectDirectory.dir("../../../../bindgen"))

    generateFromLibrary()
}

kotlin {
    jvmToolchain(17)

    jvm()

    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
        }

        commonMain {
            dependencies {
                implementation(project(":runtime"))
                implementation(project(":tests:uniffi:multi-module:rust-common"))
            }
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