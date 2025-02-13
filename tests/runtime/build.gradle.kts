import io.gitlab.trixnity.gradle.rust.dsl.useRustUpLinker

plugins {
    id("uniffi-tests-from-library")
    id("com.android.library")
}

uniffi {
    bindgenFromPath(rootProject.layout.projectDirectory.dir("bindgen"))

    generateFromLibrary {
        namespace = "runtime_test"
    }
}

kotlin {
    androidTarget()

    iosSimulatorArm64 {
        binaries.framework {
            baseName = "uniffi-runtime"
            isStatic = true
        }

        compilations.getByName("main") {
            useRustUpLinker()
        }
    }
}

android {
    namespace = "runtime_test"
    compileSdk = 34
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
