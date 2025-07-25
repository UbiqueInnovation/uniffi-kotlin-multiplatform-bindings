import ch.ubique.uniffi.plugin.extensions.useRustUpLinker
import ch.ubique.uniffi.plugin.model.RustHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.atomicfu)
    `maven-publish`
    alias(libs.plugins.maven.publish)
    id("ch.ubique.uniffi.plugin")
}

cargo {
    packageDirectory = project.layout.projectDirectory
}

uniffi {
    bindgenFromPath(
        rootProject.layout.projectDirectory.dir("bindgen-bootstrap"),
        packageName = "uniffi_bindgen_kotlin_multiplatform_bootstrap"
    )

    addRuntime = false

    generateFromLibrary()
}

kotlin {
    jvmToolchain(17)

    jvm()

    arrayOf(
        mingwX64(),
    ).forEach { nativeTarget ->
        nativeTarget.compilations.getByName("test") {
            useRustUpLinker()
        }
    }

    androidTarget {
        publishLibraryVariants("release")
    }

    linuxX64()
    linuxArm64()

    if (RustHost.Platform.MacOS.isCurrent) {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
            iosX64(),
            macosArm64(),
            macosX64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "uniffi-runtime"
                isStatic = true
            }

            iosTarget.compilations.getByName("main") {
                useRustUpLinker()
            }
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
        }

        androidMain.dependencies {
            implementation(libs.androidx.annotation)
        }
    }
}

android {
    namespace = "uniffi.runtime"
    compileSdk = 34
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

apply(from = "../gradle/artifactory.gradle")

group = property("GROUP").toString()
description = property("POM_DESCRIPTION").toString()
version = getProjectVersion()

publishing {
    repositories {
        maven {
            mavenLocal()
        }
    }
}

mavenPublishing {
    publishToMavenCentral(true)

    val disableSigning = hasProperty("disableSigning") && property("disableSigning") == "true"

    if (!disableSigning) {
        signAllPublications()
    }
}

private fun getProjectVersion(): String {
    val versionFromGradleProperties = property("VERSION").toString()
    val versionFromWorkflow = runCatching { property("githubRefName").toString().removePrefix("v") }.getOrNull()
    return versionFromWorkflow ?: versionFromGradleProperties
}
