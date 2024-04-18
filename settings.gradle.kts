pluginManagement {
    includeBuild("build-logic")

    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "uniffi-kotlin-multiplatform-bindings"

include(":tests:callbacks")
include(":tests:chronological")
include(":tests:coverall")
include(":tests:external-types")
include(":tests:futures")
include(":tests:keywords")
include(":tests:proc-macro")
include(":tests:trait-methods")

include(":tests:gradle:android-linking")
include(":tests:gradle:cargo-only")
include(":tests:gradle:no-uniffi-block")

include(":examples:app")
include(":examples:arithmetic-procmacro")
include(":examples:audio-cpp-app")
include(":examples:todolist")
include(":examples:tokio-blake3-app")
