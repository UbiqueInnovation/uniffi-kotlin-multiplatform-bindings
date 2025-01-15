plugins {
    id("uniffi-tests-from-library")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }
    }
}