import ch.ubique.uniffi.plugin.extensions.useRustUpLinker
import ch.ubique.uniffi.plugin.model.RustHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.atomicfu)
    id("ch.ubique.uniffi-plugin")

    `maven-publish`
}

cargo {
    packageDirectory = project.layout.projectDirectory
}

uniffi {
    bindgenFromPath(
        rootProject.layout.projectDirectory.dir("bindgen-bootstrap"),
        packageName = "uniffi_bindgen_kotlin_multiplatform_bootstrap"
    )

    addRuntime = false

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

    androidTarget {
        publishLibraryVariants("release")
    }

    // hostNativeTarget()

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

        androidMain.dependencies {
            implementation(libs.androidx.annotation)
        }

        listOf(
            mingwX64Main.get(),
            linuxX64Main.get(),
            linuxArm64Main.get(),
            macosArm64Main.get(),
            macosX64Main.get(),
            iosArm64Main.get(),
            iosSimulatorArm64Main.get(),
            iosX64Main.get(),
        ).forEach {
            it.kotlin.srcDir(project.layout.projectDirectory.dir("src/unifiedNativeMain"))
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

apply(from = "../gradle/artifactory.gradle")

group = "ch.ubique.uniffi"
version = "0.1.0"

publishing {
    repositories {
        maven {
            mavenLocal()
        }
    }
}
