pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "uniffi-kotlin-multiplatform-bindings"

include(":tests:callbacks")
include(":tests:coverall")
include(":tests:external_types")
include(":tests:futures")
