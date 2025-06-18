import ch.ubique.uniffi.plugin.extensions.useRustUpLinker
import ch.ubique.uniffi.plugin.model.RustHost
import org.gradle.accessors.dm.*

plugins {
    kotlin("multiplatform")
    id("ch.ubique.uniffi-plugin")
}

uniffi {
    bindgenFromPath(rootProject.layout.projectDirectory.dir("bindgen"))

    addRuntime = false
}

kotlin {
    jvm()

    mingwX64 {
        compilations.getByName("test") {
            useRustUpLinker()
        }
    }

    linuxX64()
    linuxArm64()

    if (RustHost.Platform.MacOS.isCurrent) {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
            iosX64(),
            macosArm64(),
            macosX64(),
        ).forEach { target ->
            target.compilations.getByName("main") {
                useRustUpLinker()
            }
        }
    }

    sourceSets {
        // https://github.com/gradle/gradle/issues/15383 and https://github.com/gradle/gradle/issues/19813
        val libs = project.extensions.getByName("libs") as LibrariesForLibs

        commonMain.dependencies {
            implementation(project(":runtime"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
        }
    }

    jvmToolchain(17)
}
