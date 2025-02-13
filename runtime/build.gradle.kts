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
    bindgenFromPath(rootProject.layout.projectDirectory.dir("bindgen-bootstrap"))

    generateFromLibrary()
}

kotlin {
    jvmToolchain(17)

    jvm()
    
    androidTarget()

    hostNativeTarget()

    linuxX64()
    linuxArm64()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "uniffi-runtime"
            isStatic = true
        }

        iosTarget.compilations.getByName("main") {
            useRustUpLinker()
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
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
