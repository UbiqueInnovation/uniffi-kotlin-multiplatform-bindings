import io.gitlab.trixnity.gradle.rust.dsl.hostNativeTarget

plugins {
    kotlin("multiplatform")

    id("io.gitlab.trixnity.rust.kotlin.multiplatform")
}

kotlin {
    jvm()

    hostNativeTarget()

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
