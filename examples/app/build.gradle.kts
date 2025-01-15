import io.gitlab.trixnity.gradle.CargoHost
import io.gitlab.trixnity.gradle.rustlink.useRustUpLinker

plugins {
    kotlin("multiplatform")
    id("io.gitlab.trixnity.rustlink.kotlin.multiplatform")
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget()
    jvm()
    arrayOf(
        mingwX64(),
    ).forEach {
        it.binaries.executable {
            entryPoint = "io.gitlab.trixnity.uniffi.examples.app.main"
        }
        for (compilation in it.compilations) {
            compilation.useRustUpLinker()
        }
    }

    // TODO: Generate .def file with pkg-config automatically
    // macOS: brew install pkg-config gtk4
    // Debian: apt install pkg-config libgtk-4-dev
    //
    // headers = gtk/gtk.h
    // compilerOpts = $(pkg-config --cflags gtk4)
    // linkerOpts = $(pkg-config --libs gtk4)
    //
    // TODO: Support cross-compilation
    // arrayOf(
    //     linuxX64(),
    //     linuxArm64(),
    // ).forEach {
    //     it.binaries.executable {
    //         entryPoint = "io.gitlab.trixnity.uniffi.examples.app.main"
    //     }
    //     it.compilations.getByName("main") {
    //         cinterops.register("gtk") {
    //             defFile("src/linuxMain/cinterop/gtk.def")
    //             packageName("org.gnome.gitlab.gtk")
    //         }
    //     }
    // }

    if (CargoHost.Platform.MacOS.isCurrent) {
        arrayOf(
            macosArm64(),
            macosX64(),
        ).forEach {
            it.binaries.executable {
                entryPoint = "io.gitlab.trixnity.uniffi.examples.app.main"
            }
        }

        arrayOf(
            iosArm64(),
            iosSimulatorArm64(),
            iosX64(),
            macosArm64(),
            macosX64(),
        ).forEach {
            it.binaries.framework {
                baseName = "ExamplesAppKotlin"
                isStatic = true
                binaryOption("bundleId", "io.gitlab.trixnity.uniffi.examples.app.kotlin")
                binaryOption("bundleVersion", "0")
                export(project(":examples:arithmetic-procmacro"))
                export(project(":examples:todolist"))
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":examples:arithmetic-procmacro"))
            api(project(":examples:todolist"))
            implementation(compose.runtime)
        }

        commonTest {
            // TODO: Test the following in a dedicated test, not in an example. See #52 for more details.
            kotlin.srcDir(project.layout.projectDirectory.dir("../arithmetic-procmacro/src/commonTest/kotlin"))
            kotlin.srcDir(project.layout.projectDirectory.dir("../todolist/src/commonTest/kotlin"))
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        androidMain.dependencies {
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace = "io.gitlab.trixnity.uniffi.examples.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.gitlab.trixnity.uniffi.examples.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        ndk.abiFilters.add("arm64-v8a")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}
