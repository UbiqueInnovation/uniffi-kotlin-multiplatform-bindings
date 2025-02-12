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
        commonMain {
            dependencies {
                implementation(project(":runtime"))
            }
        }
    }
}