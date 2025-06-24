plugins {
    id("uniffi-tests-from-library")
    alias(libs.plugins.kotlinx.serialization)
}

uniffi.formatCode = true

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.serialization.json)
    }
}