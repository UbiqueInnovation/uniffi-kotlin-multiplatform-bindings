import io.gitlab.trixnity.gradle.rust.dsl.hostNativeTarget

plugins {
    id("io.gitlab.trixnity.rust.kotlin.multiplatform")
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(17)
    jvm()
    hostNativeTarget()

    sourceSets {
        // https://github.com/gradle/gradle/issues/15383 and https://github.com/gradle/gradle/issues/19813
        val libs = project.extensions.getByName("libs") as org.gradle.accessors.dm.LibrariesForLibs

        commonTest.dependencies {
            implementation(project(":tests:uniffi:multi-module:rust-common"))
            // api(project(":tests:uniffi:multi-module:mod-a"))
            // api(project(":tests:uniffi:multi-module:mod-b"))

            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
        }
    }
}