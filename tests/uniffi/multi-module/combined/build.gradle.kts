plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)

            implementation(project(":tests:uniffi:multi-module:rust-common"))
            implementation(project(":tests:uniffi:multi-module:mod-a"))
            implementation(project(":tests:uniffi:multi-module:mod-b"))
        }
    }
}
