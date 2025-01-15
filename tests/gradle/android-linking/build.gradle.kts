import com.android.build.gradle.internal.tasks.factory.dependsOn
import io.gitlab.trixnity.gradle.CargoHost
import io.gitlab.trixnity.gradle.cargo.dsl.android
import io.gitlab.trixnity.gradle.cargo.rust.targets.RustAndroidTarget

plugins {
    kotlin("multiplatform")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform")
    alias(libs.plugins.android.library)
}

// Build a library manually to test passing an absolute path to `ndkLibraries` works well
val anotherCustomCppLibraryRoot: Directory = project.layout.projectDirectory.dir("another-android-linking-cpp")
val androidTargets = RustAndroidTarget.values()
val anotherCustomCppLibraryCmakeOutputDirectories = androidTargets.associateWith {
    project.layout.buildDirectory.dir("intermediates/ninja/project/debug/${it.androidAbiName}").get()
}
val anotherCustomCppLibraryLocations = anotherCustomCppLibraryCmakeOutputDirectories.mapValues {
    it.value.file("libanother-android-linking-cpp.so")
}
val androidSdkCMakeDirectory = android.sdkDirectory
    .resolve("cmake")
    .listFiles()
    .first { file -> file.name.startsWith("3.") }
    .resolve("bin")
val androidSdkCMake = androidSdkCMakeDirectory.resolve(CargoHost.Platform.current.convertExeName("cmake"))
val androidSdkNinja = androidSdkCMakeDirectory.resolve(CargoHost.Platform.current.convertExeName("ninja"))
val anotherCustomCppLibraryBuildTasks = androidTargets.associateWith {
    val cmakeOutputDirectory = anotherCustomCppLibraryCmakeOutputDirectories[it]!!
    val libraryLocation = anotherCustomCppLibraryLocations[it]!!
    val configureTask = tasks.register<Exec>("configureCustomCppLibraryCMake${it.friendlyName}") {
        commandLine(
            androidSdkCMake,
            "-H$anotherCustomCppLibraryRoot",
            "-B$cmakeOutputDirectory",
            "-DANDROID_ABI=${it.androidAbiName}",
            "-DANDROID_PLATFORM=29",
            "-DANDROID_NDK=${android.ndkDirectory}",
            "-DCMAKE_TOOLCHAIN_FILE=${android.ndkDirectory}/build/cmake/android.toolchain.cmake",
            "-DCMAKE_MAKE_PROGRAM=$androidSdkNinja",
            "-G Ninja",
        )

        inputs.dir(anotherCustomCppLibraryRoot)
        outputs.dir(cmakeOutputDirectory)
    }

    tasks.register<Exec>("buildCustomCppLibrary${it.friendlyName}") {
        commandLine(
            androidSdkCMake,
            "--build",
            "$cmakeOutputDirectory"
        )
        dependsOn(configureTask)

        inputs.dir(cmakeOutputDirectory)
        outputs.file(libraryLocation)
    }
}

cargo {
    builds.android {
        val anotherCustomCppLibraryBuildTask = anotherCustomCppLibraryBuildTasks[rustTarget]!!
        val libraryLocation = anotherCustomCppLibraryLocations[rustTarget]!!
        ndkLibraries.addAll("c++_shared", libraryLocation.asFile.absolutePath)
        variants {
            findNdkLibrariesTaskProvider.dependsOn(anotherCustomCppLibraryBuildTask)
        }
    }
}

kotlin {
    androidTarget()
    sourceSets {
        getByName("androidInstrumentedTest") {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("androidx.test:core:1.5.0")
                implementation("androidx.test:runner:1.5.2")
            }
        }
    }
}

android {
    namespace = "io.gitlab.trixnity.uniffi.tests.gradle.androidlinking"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    externalNativeBuild {
        cmake {
            path = File("CMakeLists.txt")
        }
    }
}
