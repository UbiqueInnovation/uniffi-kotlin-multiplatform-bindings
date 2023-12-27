/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.gradle

import com.sun.jna.*
import io.gitlab.trixnity.uniffi.gradle.tasks.*
import io.gitlab.trixnity.uniffi.gradle.utils.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*

private const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
private const val KOTLIN_ATOMIC_FU_PLUGIN_ID = "org.jetbrains.kotlin.plugin.atomicfu"

private const val TASK_GROUP = "uniffi"

class UniFfiPlugin : Plugin<Project> {
    private lateinit var uniFfiExtension: UniFfiExtension

    override fun apply(target: Project): Unit = with(target) {
        uniFfiExtension = extensions.create<UniFfiExtension>(TASK_GROUP, this)

        afterEvaluate {
            val generation = uniFfiExtension.bindingsGeneration.orNull

            if (generation == null) {
                logger.warn(
                    "No bindings generation defined. " +
                            "Please use either a `generateFromUdl` or `generateFromLibrary` block."
                )
                return@afterEvaluate
            }

            ensureRequiredPlugin("Kotlin Multiplatform", KOTLIN_MULTIPLATFORM_PLUGIN_ID)
            ensureRequiredPlugin("Kotlin AtomicFU", KOTLIN_ATOMIC_FU_PLUGIN_ID)

            val crateDirectory = generation.crateDirectory.get()
            val profile = generation.profile.get()

            val cargoMetadata = getCargoMetadata(crateDirectory)
            val cargoTargetDir = File(cargoMetadata.targetDirectory).resolve(profile)

            val generatedBindingsDir = layout.buildDirectory.dir("generated/uniffi").get()

            configureTasks(generation, cargoTargetDir, generatedBindingsDir)

            plugins.withId(KOTLIN_MULTIPLATFORM_PLUGIN_ID) {
                val kotlinMultiplatformExtension = extensions.getByType<KotlinMultiplatformExtension>()
                kotlinMultiplatformExtension.configureKotlin(generation, cargoTargetDir, generatedBindingsDir)
            }
        }
    }

    private fun Project.configureTasks(
        generation: BindingsGeneration,
        cargoTargetDir: File,
        generatedBindingsDir: Directory,
    ) {
        val buildCrate = tasks.register<BuildCrateTask>("buildCrate") {
            group = TASK_GROUP

            crateDirectory.set(generation.crateDirectory)
            libraryName.set(generation.libraryName)
            profile.set(generation.profile)
            targetDirectory.set(cargoTargetDir)
        }

        val cleanCrate = tasks.register<Exec>("cleanCrate") {
            group = TASK_GROUP

            workingDir(generation.crateDirectory)
            commandLine("cargo", "clean")
        }

        val installBindgen = tasks.register<InstallBindgenTask>("installBindgen") {
            group = TASK_GROUP

            bindgenCratePath.set(uniFfiExtension.bindgenCratePath)
            installDirectory.set(layout.buildDirectory.dir("bindgen-install"))
        }

        val buildBindings =
            tasks.register<BuildBindingsTask>("buildBindings") {
                group = TASK_GROUP

                bindgen.set(installBindgen.get().bindgen)
                outputDirectory.set(generatedBindingsDir)
                libraryName.set(generation.libraryName)

                // TODO Understand why setting this makes the binding generation fail
                // crateName.set(generation.crateName)

                when (generation) {
                    is BindingsGenerationFromUdl -> {
                        libraryMode.set(false)
                        if (generation.config.isPresent) config.set(generation.config)
                        libraryFile.set(buildCrate.get().libraryFile)
                        source.set(generation.udlFile)
                    }

                    is BindingsGenerationFromLibrary -> {
                        libraryMode.set(true)
                        source.set(buildCrate.get().libraryFile)
                    }

                    else -> throw GradleException("Invalid bindings generation class")
                }
                dependsOn(buildCrate, installBindgen)
            }

        val cleanBindings = tasks.register<Delete>("cleanBindings") {
            group = TASK_GROUP

            delete(generatedBindingsDir)
        }

        val copyBinaries = tasks.register<Copy>("copyBinaries") {
            group = TASK_GROUP

            from(buildCrate.get().libraryFile)
            into(layout.buildDirectory.dir("processedResources/jvm/main/${Platform.RESOURCE_PREFIX}"))
            dependsOn(buildCrate)
        }

        tasks.withType<ProcessResources> {
            dependsOn(copyBinaries)
        }

        tasks.withType<KotlinCompile<*>> {
            dependsOn(buildBindings)
        }

        tasks.withType<CInteropProcess> {
            dependsOn(copyBinaries, buildBindings)
        }

        tasks.named<Delete>("clean") {
            dependsOn(cleanBindings, cleanCrate)
        }
    }

    private fun KotlinMultiplatformExtension.configureKotlin(
        generation: BindingsGeneration,
        cargoTargetDir: File,
        generatedBindingsDir: Directory,
    ) {
        var hasJvmTarget = false
        var hasNativeTargets = false

        targets.all { target ->
            target.compilations.getByName("main") { compilation ->
                compilation.kotlinOptions {
                    freeCompilerArgs += "-Xexpect-actual-classes"
                }
            }

            if (target.name == "jvm") hasJvmTarget = true
            if (target is KotlinNativeTarget) hasNativeTargets = true
        }

        sourceSets.configureEach { sourceSet ->
            sourceSet.languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }

            if (sourceSet.name == "commonMain") {
                sourceSet.kotlin.srcDir(generatedBindingsDir.dir("commonMain/kotlin"))

                sourceSet.dependencies {
                    implementation("com.squareup.okio:okio:${DependencyVersions.OKIO}")
                    implementation("org.jetbrains.kotlinx:atomicfu:${DependencyVersions.KOTLINX_ATOMICFU}")
                    implementation("org.jetbrains.kotlinx:kotlinx-datetime:${DependencyVersions.KOTLINX_DATETIME}")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${DependencyVersions.KOTLINX_COROUTINES}")
                }
            }

            if (sourceSet.name == "jvmMain" && hasJvmTarget) {
                sourceSet.kotlin.srcDir(generatedBindingsDir.dir("jvmMain/kotlin"))

                sourceSet.dependencies {
                    implementation("net.java.dev.jna:jna:${DependencyVersions.JNA}")
                }
            }

            if (sourceSet.name == "nativeMain" && hasNativeTargets) {
                sourceSet.kotlin.srcDir(generatedBindingsDir.dir("nativeMain/kotlin"))
            }
        }

        val libraryName = generation.libraryName.get()
        val namespace = generation.namespace.get()

        targets.all { target ->
            if (target is KotlinNativeTarget) {
                target.compilations.getByName("main") { compilation ->
                    compilation.cinterops { cinterop ->
                        cinterop.register(TASK_GROUP) { settings ->
                            settings.packageName("$namespace.cinterop")
                            settings.defFile(generatedBindingsDir.file("nativeInterop/cinterop/$libraryName.def"))
                            settings.header(generatedBindingsDir.dir("nativeInterop/cinterop/headers/$namespace/$namespace.h"))
                            settings.extraOpts("-libraryPath", cargoTargetDir.absolutePath)
                        }
                    }
                }
            }
        }
    }
}

private fun Project.ensureRequiredPlugin(name: String, id: String) {
    if (!plugins.hasPlugin(id)) {
        logger.error(requiredPluginMessage(name, id))
        throw GradleException("No $name Gradle plugin found")
    }
}

private fun requiredPluginMessage(name: String, id: String): String {
    return """
        Please include the $name Gradle plugin in your build configuration.

        plugins {
          // ...
          id("$id")
          // ...
        }
    """.trimIndent()
}
