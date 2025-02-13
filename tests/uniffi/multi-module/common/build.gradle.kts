plugins {
    id("uniffi-tests-from-library")
}

uniffi {
    bindgenFromPath(rootProject.layout.projectDirectory.dir("bindgen"))

    generateFromLibrary {
        namespace = "runtime_test"
    }
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(project(":tests:uniffi:multi-module:rust-common"))
            // api(project(":tests:uniffi:multi-module:mod-a"))
            // api(project(":tests:uniffi:multi-module:mod-b"))

            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
        }
    }
}
