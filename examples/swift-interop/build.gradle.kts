import ch.ubique.uniffi.plugin.extensions.useRustUpLinker
import ch.ubique.uniffi.plugin.model.RustHost
import ch.ubique.uniffi.plugin.tasks.GenerateDefFileTask
import ch.ubique.uniffi.plugin.tasks.GenerateDummyDefFileTask
import io.github.frankois944.spmForKmp.swiftPackageConfig
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.spm4kmp)
    id("ch.ubique.uniffi.plugin")
}

uniffi {
    bindgenFromPath(rootProject.layout.projectDirectory.dir("bindgen"))

    generateFromLibrary()
}

kotlin {
    jvmToolchain(17)

    // spmForKmp only does anything on Apple targets, but the project still has to configure on other
    // hosts, and a Kotlin Multiplatform project needs at least one target to do that.
    jvm()

    if (RustHost.Platform.MacOS.isCurrent) {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
            iosX64(),
        ).forEach { iosTarget ->
            iosTarget.compilations.getByName("main") {
                useRustUpLinker()
            }

            iosTarget.swiftPackageConfig(cinteropName = "swiftGreeter") {
                minIos = "16.4"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":runtime"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions.core)
        }
    }
}

/*
 * Workaround for a conflict between spmForKmp and this plugin. See https://github.com/frankois944/spm4Kmp/issues/326
 */
afterEvaluate {
    kotlin.targets.withType<KotlinNativeTarget>().configureEach {
        val defFile = uniffiDefFileFor(targetName)

        compilations.getByName("main").cinterops.named("uniffi-cinterop") {
            definitionFile.set(defFile)

            tasks.named(interopProcessingTaskName) {
                dependsOn(defFile)
            }
        }
    }
}

/**
 * The def file the plugin feeds to its cinterop for [targetName].
 *
 * This has to mirror the branch in `UniffiPlugin`: during an IDE sync the plugin registers a single
 * [GenerateDummyDefFileTask] shared by every target and skips building the rust library, otherwise it
 * registers one [GenerateDefFileTask] per target.
 */
fun uniffiDefFileFor(targetName: String): Provider<RegularFile> {
    // idea.sync.active is automatically set by any idea IDE
    val isSync = providers.systemProperty("idea.sync.active").map(String::toBoolean).getOrElse(false)

    return if (isSync) {
        tasks.named<GenerateDummyDefFileTask>("generateDummyDefFile")
            .flatMap { it.outputFile }
    } else {
        tasks.named<GenerateDefFileTask>(
            "generateDefFileFor${targetName.replaceFirstChar(Char::uppercaseChar)}"
        ).flatMap { it.outputFile }
    }
}
