pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "uniffi-kotlin-multiplatform-bindings"

include(":tests:callbacks")
include(":tests:coverall")
include(":tests:external-types")
include(":tests:futures")
include(":tests:keywords")
include(":tests:proc-macro")
include(":tests:trait-methods")
