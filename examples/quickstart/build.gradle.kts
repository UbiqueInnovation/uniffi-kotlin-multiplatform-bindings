import dev.oxikt.gradle.plugin.extensions.useRustUpLinker
import dev.oxikt.gradle.plugin.model.RustHost

plugins {
    kotlin("multiplatform")
	id("dev.oxikt.gradle.plugin")
}

uniffi {
    bindgenFromPath(rootProject.layout.projectDirectory.dir("bindgen"))

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

    linuxX64()
    linuxArm64()

    if (RustHost.Platform.MacOS.isCurrent) {
        iosArm64()
        iosSimulatorArm64()
        iosX64()
        macosArm64()
    }

    sourceSets {
        commonMain.dependencies {
			implementation(project(":runtime"))
        }

        commonTest.dependencies {
			implementation(kotlin("test"))
			implementation(libs.kotest.assertions.core)
        }
    }
}
