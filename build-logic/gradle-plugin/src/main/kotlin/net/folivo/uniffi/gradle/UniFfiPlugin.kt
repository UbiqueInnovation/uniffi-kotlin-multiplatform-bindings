/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package net.folivo.uniffi.gradle

import com.sun.jna.Platform
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import java.io.File

private const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
private const val TASK_GROUP = "uniffi"

class UniFfiPlugin : Plugin<Project> {
    private lateinit var uniFfiExtension: UniFfiExtension

    override fun apply(target: Project): Unit = with(target) {
        uniFfiExtension = extensions.create<UniFfiExtension>(TASK_GROUP)

        afterEvaluate {
            val crateDirectory = uniFfiExtension.crateDirectory.get()
            val crateName = uniFfiExtension.crateName.get()
            val libraryName = uniFfiExtension.libraryName.getOrElse(crateName)
            val namespace = uniFfiExtension.namespace.getOrElse(crateName)
            val profile = uniFfiExtension.profile.getOrElse("debug")

            val cargoMetadata = getCargoMetadata(crateDirectory)
            val cargoTargetDir = File(cargoMetadata.targetDirectory).resolve(profile)

            val generatedBindingsDir = layout.buildDirectory.dir("generated/uniffi").get()

            configureTasks(crateName, libraryName, profile, cargoTargetDir, generatedBindingsDir)

            if (!plugins.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID)) {
                throw GradleException("You must add the Kotlin Multiplatform Gradle plugin")
            }

            plugins.apply("org.jetbrains.kotlin.plugin.atomicfu")

            plugins.withId(KOTLIN_MULTIPLATFORM_PLUGIN_ID) {
                val kotlinMultiplatformExtension = extensions.getByType<KotlinMultiplatformExtension>()
                kotlinMultiplatformExtension.configureKotlin(
                    crateName = crateName,
                    namespace = namespace,
                    cargoTargetDir = cargoTargetDir,
                    generatedBindingsDir = generatedBindingsDir
                )
            }
        }
    }

    private fun Project.configureTasks(
        theCrateName: String,
        theLibraryName: String,
        theProfile: String,
        cargoTargetDir: File,
        generatedBindingsDir: Directory,
    ) {
        val theCrateDirectory = uniFfiExtension.crateDirectory

        val buildCrate = tasks.register<BuildCrateTask>("buildCrate") {
            group = TASK_GROUP

            crateDirectory.set(theCrateDirectory)
            crateName.set(theCrateName)
            profile.set(theProfile)
            targetDirectory.set(cargoTargetDir)
        }

        val cleanCrate = tasks.register<Exec>("cleanCrate") {
            group = TASK_GROUP

            workingDir(theCrateDirectory)
            commandLine("cargo", "clean")
        }

        val installBindgen = tasks.register<InstallBindgenTask>("installBindgen") {
            group = TASK_GROUP

            bindgenCratePath.set(uniFfiExtension.bindgenCratePath)
            installDirectory.set(layout.buildDirectory.dir("bindgen-install"))
        }

        val theUdlFile = uniFfiExtension.udlFile.convention(
            theCrateDirectory.file("src/${theCrateName}.udl")
        )

        val buildBindings = tasks.register<BuildBindingsTask>("buildBindings") {
            group = TASK_GROUP

            bindgen.set(installBindgen.get().bindgen)
            udlFile.set(theUdlFile)
            libraryFile.set(buildCrate.get().libraryFile)
            outputDirectory.set(generatedBindingsDir)
            crateName.set(theCrateName)
            libraryName.set(theLibraryName)

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
        crateName: String,
        namespace: String,
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

        targets.all { target ->
            if (target is KotlinNativeTarget) {
                target.compilations.getByName("main") { compilation ->
                    compilation.cinterops { cinterop ->
                        cinterop.register(TASK_GROUP) { settings ->
                            settings.packageName("$namespace.cinterop")
                            settings.defFile(generatedBindingsDir.file("nativeInterop/cinterop/$crateName.def"))
                            settings.header(generatedBindingsDir.dir("nativeInterop/cinterop/headers/$namespace/$namespace.h"))
                            settings.extraOpts("-libraryPath", cargoTargetDir.absolutePath)
                        }
                    }
                }
            }
        }
    }
}
