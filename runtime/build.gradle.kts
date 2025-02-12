plugins {
    id("uniffi-tests-from-library")
}

uniffi {
    bindgenFromPath(layout.projectDirectory.dir("../bindgen-bootstrap"))

    generateFromLibrary {
        namespace = "uniffi_runtime"
    }
}

/*
import io.gitlab.trixnity.gradle.rust.dsl.hostNativeTarget
import io.gitlab.trixnity.gradle.rust.dsl.useRustUpLinker

plugins {
    kotlin("multiplatform")
    id("com.android.library")

    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform")
    id("io.gitlab.trixnity.rust.kotlin.multiplatform")
    alias(libs.plugins.kotlin.atomicfu)
}

uniffi {
    bindgenFromPath(layout.projectDirectory.dir("../bindgen-bootstrap"))

    generateFromLibrary()
}

kotlin {
    jvmToolchain(17)

    jvm()
    
    androidTarget()

    // hostNativeTarget()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "uniffi-runtime"
            isStatic = true
        }

        iosTarget.binaries.all {
            freeCompilerArgs += "-Xallocator=mimalloc"
        }

        iosTarget.compilations.getByName("main") {
            useRustUpLinker()
        }
    }

    sourceSets {
        androidMain.dependencies {
        }

        commonMain.dependencies {

        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
        }

        jvmMain.dependencies {
        }

        nativeMain.dependencies {

        }
    }
}

android {
    namespace = "uniffi.runtime"
    compileSdk = 34
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
*/
