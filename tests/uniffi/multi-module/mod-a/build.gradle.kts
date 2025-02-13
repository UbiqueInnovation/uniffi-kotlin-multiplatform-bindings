plugins {
    id("uniffi-tests-from-library")
}

uniffi {
    bindgenFromPath(rootProject.layout.projectDirectory.dir("bindgen"))

    generateFromLibrary {
        namespace = "module_a"
    }
}

kotlin {
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
