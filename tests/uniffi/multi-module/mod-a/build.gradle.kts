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
        commonMain {
            dependencies {
                implementation(project(":tests:uniffi:multi-module:rust-common"))
            }
        }
    }
}
