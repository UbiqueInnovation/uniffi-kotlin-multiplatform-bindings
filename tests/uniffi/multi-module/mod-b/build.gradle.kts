plugins {
    id("uniffi-tests-from-library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":tests:uniffi:multi-module:rust-common"))
        }
    }
}