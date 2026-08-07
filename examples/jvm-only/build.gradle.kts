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
