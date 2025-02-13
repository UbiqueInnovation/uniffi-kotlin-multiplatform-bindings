import io.gitlab.trixnity.gradle.RustHost
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

    arrayOf(
        mingwX64(),
    ).forEach { nativeTarget ->
        nativeTarget.compilations.getByName("test") {
            useRustUpLinker()
        }
    }

    androidTarget()

    hostNativeTarget()

    linuxX64()
    linuxArm64()

    if (RustHost.Platform.MacOS.isCurrent) {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
            iosX64(),
            macosArm64(),
            macosX64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "uniffi-runtime"
                isStatic = true
            }

            iosTarget.compilations.getByName("main") {
                useRustUpLinker()
            }
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
