plugins {
    kotlin("multiplatform")
    id("host-jvm-native-tests")
    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
        }
    }
}