import io.gitlab.trixnity.gradle.CargoHost
import io.gitlab.trixnity.gradle.cargo.dsl.jvm
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("host-jvm-native-tests")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform")
}

cargo {
    builds.jvm {
        jvm = (rustTarget == CargoHost.current.hostTarget)
        resourcePrefix = "jvm"
    }
}

kotlin.targets.withType(KotlinNativeTarget::class) {
    compilations.getByName("main") {
        cinterops.register("rustBindings") {
            includeDirs(layout.buildDirectory.dir("generated"))
        }
    }
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
    }
}
