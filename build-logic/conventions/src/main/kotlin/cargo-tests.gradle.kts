import io.gitlab.trixnity.gradle.RustHost
import io.gitlab.trixnity.gradle.cargo.dsl.jvm
import io.gitlab.trixnity.gradle.cargo.dsl.native
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("host-jvm-native-tests")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform")
}

cargo {
    builds.jvm {
        jvm = (rustTarget == RustHost.current.rustTarget)
        resourcePrefix = "jvm"
    }
}

kotlin.targets.withType(KotlinNativeTarget::class) {
    val kotlinTarget = this
    compilations.getByName("main") {
        cinterops.register("rustBindings") {
            includeDirs(layout.buildDirectory.dir("generated"))
            tasks.named(interopProcessingTaskName) {
                cargo.builds.native.configureEach {
                    if (kotlinTargets.contains(kotlinTarget)) {
                        dependsOn(variant(nativeVariant.get()).buildTaskProvider)
                    }
                }
            }
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
