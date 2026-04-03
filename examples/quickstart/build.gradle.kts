import ch.ubique.uniffi.plugin.extensions.useRustUpLinker
import ch.ubique.uniffi.plugin.model.RustHost

plugins {
    kotlin("multiplatform")
	id("ch.ubique.uniffi.plugin")
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
        macosX64()
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
