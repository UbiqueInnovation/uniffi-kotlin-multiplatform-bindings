plugins {
    id("uniffi-tests-from-library")
}

uniffi {
    bindgenFromPath(rootProject.layout.projectDirectory.dir("bindgen-common"))

    generateFromLibrary {
        namespace = "rust_common"
    }
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
        }
    }
}