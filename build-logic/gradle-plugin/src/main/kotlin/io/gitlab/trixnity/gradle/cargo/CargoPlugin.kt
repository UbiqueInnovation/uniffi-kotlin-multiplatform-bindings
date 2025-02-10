/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.tasks.ProcessJavaResTask
import com.android.build.gradle.tasks.MergeSourceSetFolders
import io.gitlab.trixnity.gradle.*
import io.gitlab.trixnity.gradle.cargo.dsl.*
import io.gitlab.trixnity.gradle.cargo.rust.CrateType
import io.gitlab.trixnity.gradle.cargo.rust.targets.RustAndroidTarget
import io.gitlab.trixnity.gradle.cargo.rust.targets.RustJvmTarget
import io.gitlab.trixnity.gradle.cargo.rust.targets.RustTarget
import io.gitlab.trixnity.gradle.cargo.tasks.CargoCleanTask
import io.gitlab.trixnity.gradle.cargo.tasks.CargoTask
import io.gitlab.trixnity.gradle.cargo.tasks.RustUpTargetAddTask
import io.gitlab.trixnity.gradle.cargo.tasks.RustUpTask
import io.gitlab.trixnity.gradle.tasks.useGlobalLock
import io.gitlab.trixnity.gradle.utils.DependencyUtils
import io.gitlab.trixnity.gradle.utils.PluginUtils
import io.gitlab.trixnity.gradle.utils.register
import io.gitlab.trixnity.uniffi.gradle.PluginIds
import org.gradle.api.*
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import java.io.File

class CargoPlugin : Plugin<Project> {
    companion object {
        internal const val TASK_GROUP = "cargo"
    }

    private lateinit var cargoExtension: CargoExtension
    private lateinit var kotlinMultiplatformExtension: KotlinMultiplatformExtension
    private lateinit var androidExtension: BaseExtension
    private var androidMinSdk = 21
    private var androidNdkRoot: File? = null
    private var androidNdkVersion: String? = null

    override fun apply(target: Project) {
        cargoExtension = target.extensions.create<CargoExtension>(TASK_GROUP, target)
        cargoExtension.jvmVariant.convention(Variant.Debug)
        cargoExtension.nativeVariant.convention(Variant.Debug)
        readVariantsFromXcode()
        cargoExtension.builds.native {
            nativeVariant.convention(
                cargoExtension.nativeTargetVariantOverride.getting(rustTarget).orElse(cargoExtension.nativeVariant)
            )
        }
        cargoExtension.builds.jvm {
            jvmVariant.convention(cargoExtension.jvmVariant)
        }
        target.useGlobalLock()
        target.tasks.withType<CargoTask>().configureEach {
            it.additionalEnvironmentPath.add(cargoExtension.toolchainDirectory)
        }
        target.tasks.withType<RustUpTask>().configureEach {
            it.additionalEnvironmentPath.add(cargoExtension.toolchainDirectory)
        }
        target.watchPluginChanges()
        target.afterEvaluate {
            target.checkRequiredPlugins()
            target.checkKotlinTargets()
            applyAfterEvaluate(it)
        }
    }

    private fun applyAfterEvaluate(target: Project): Unit = with(target) {
        checkRequiredCrateTypes()
        if (cargoExtension.builds.isEmpty()) {
            logger.warn("No Kotlin targets detected.")
            return
        }

        configureBuildTasks()
        configureCleanTasks()
    }

    private fun Project.watchPluginChanges() {
        plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
            kotlinMultiplatformExtension = extensions.getByType()
            kotlinMultiplatformExtension.targets.configureEach { it.planBuilds() }
        }

        val androidPluginAction = Action<Plugin<*>> {
            androidExtension = extensions.getByType()
            val abiFilters = androidExtension.defaultConfig.ndk.abiFilters
            cargoExtension.androidTargetsToBuild.convention(project.provider {
                if (abiFilters.isNotEmpty()) {
                    abiFilters.map(::RustAndroidTarget)
                } else {
                    RustAndroidTarget.entries
                }
            })

            project.afterEvaluate {
                // TODO: Read <uses-sdk> from AndroidManifest.xml
                // androidExtension.sourceSets.getByName("main").manifest.srcFile
                androidExtension.defaultConfig.minSdk?.let {
                    androidMinSdk = it
                }
                androidExtension.ndkVersion.takeIf(String::isNotEmpty)?.let {
                    androidNdkVersion = it
                }
                androidExtension.ndkPath?.let {
                    androidNdkRoot = File(it)
                }
            }
        }

        // If either of the Android application plugin or the Android library plugin is present, retrieve the extension.
        plugins.withId(PluginIds.ANDROID_APPLICATION, androidPluginAction)
        plugins.withId(PluginIds.ANDROID_LIBRARY, androidPluginAction)
    }

    private fun Project.checkRequiredPlugins() {
        PluginUtils.ensurePluginIsApplied(this, "Kotlin Multiplatform", PluginIds.KOTLIN_MULTIPLATFORM)
    }

    private fun KotlinTarget.planBuilds() {
        for (rustTarget in requiredRustTargets()) {
            cargoExtension.createOrGetBuild(rustTarget).kotlinTargets.add(this)
        }
    }

    private fun KotlinTarget.requiredRustTargets(): List<RustTarget> {
        return when (this) {
            is KotlinJvmTarget -> RustHost.current.platform.supportedTargets.filterIsInstance<RustJvmTarget>()
            is KotlinAndroidTarget -> RustAndroidTarget.entries
            is KotlinNativeTarget -> listOf(RustTarget(konanTarget))
            else -> listOf()
        }
    }

    private fun Project.checkKotlinTargets() {
        val hasJsTargets = kotlinMultiplatformExtension.targets.any { it.platformType == KotlinPlatformType.js }
        if (hasJsTargets) {
            project.logger.warn("JS targets are added, but UniFFI KMP bindings does not support JS targets yet.")
        }

        val hasWasmTargets = kotlinMultiplatformExtension.targets.any { it.platformType == KotlinPlatformType.wasm }
        if (hasWasmTargets) {
            project.logger.warn("WASM targets are added, but UniFFI KMP bindings does not support WASM targets yet.")
        }

        val hasAndroidJvmTargets =
            kotlinMultiplatformExtension.targets.any { it.platformType == KotlinPlatformType.androidJvm }
        if (hasAndroidJvmTargets && !::androidExtension.isInitialized) {
            throw GradleException("Android JVM targets are added, but Android Gradle Plugin is not found.")
        }
    }

    private fun checkRequiredCrateTypes() {
        val requiredCrateTypes = cargoExtension
            .builds
            .flatMap { it.kotlinTargets }
            .map { it.platformType.requiredCrateType() }
            .distinct()
        val actualCrateTypes = cargoExtension.cargoPackage.get().libraryCrateTypes
        if (!actualCrateTypes.containsAll(requiredCrateTypes)) {
            throw GradleException(
                "Crate does not have required crate types. Required: $requiredCrateTypes, actual: $actualCrateTypes"
            )
        }
    }

    private fun readVariantsFromXcode() {
        val sdkName = System.getenv("SDK_NAME") ?: return
        val sdk = AppleSdk(sdkName)

        val configuration = System.getenv("CONFIGURATION") ?: return
        val variant = Variant(configuration)

        val archs = System.getenv("ARCHS")?.split(' ')?.map(AppleSdk.Companion::Arch) ?: return
        cargoExtension.nativeTargetVariantOverride.putAll(archs.mapNotNull(sdk::rustTarget).associateWith { variant })
    }

    private fun Project.configureBuildTasks() {
        val androidTarget = cargoExtension.builds.firstNotNullOfOrNull { build ->
            build.kotlinTargets.firstNotNullOfOrNull { it as? KotlinAndroidTarget }
        }
        for (cargoBuild in cargoExtension.builds) {
            val rustUpTargetAddTask = tasks.register<RustUpTargetAddTask>({ +cargoBuild.rustTarget }) {
                group = TASK_GROUP
                this.rustTarget.set(cargoBuild.rustTarget)
            }
            for (cargoBuildVariant in cargoBuild.variants) {
                cargoBuildVariant.buildTaskProvider.configure {
                    it.nativeStaticLibsDefFile.set(it.outputCacheFile("nativeStaticLibsDefFile"))
                    it.dependsOn(rustUpTargetAddTask)
                    if (cargoBuildVariant is CargoAndroidBuildVariant) {
                        val environmentVariables = cargoBuildVariant.rustTarget.ndkEnvVariables(
                            sdkRoot = androidExtension.sdkDirectory,
                            apiLevel = androidMinSdk,
                            ndkVersion = androidNdkVersion,
                            ndkRoot = androidNdkRoot,
                        )
                        it.additionalEnvironment.putAll(environmentVariables)
                    }
                }
            }
            for (kotlinTarget in cargoBuild.kotlinTargets) {
                when (kotlinTarget) {
                    is KotlinJvmTarget -> {
                        cargoBuild as CargoJvmBuild<*>
                        cargoBuild.variants {
                            configureJvmPostBuildTasks(
                                kotlinTarget,
                                // cargoBuild.jvmVariant is checked inside
                                this,
                                // required for Android local unit tests
                                androidTarget,
                            )
                        }
                    }

                    is KotlinAndroidTarget -> {
                        cargoBuild as CargoAndroidBuild
                        Variant.entries.forEach {
                            configureAndroidPostBuildTasks(cargoBuild.variant(it))
                        }
                    }

                    is KotlinNativeTarget -> {
                        cargoBuild as CargoNativeBuild<*>
                        configureNativeCompilation(
                            kotlinTarget,
                            cargoBuild.variant(cargoBuild.nativeVariant.get())
                        )
                    }
                }
            }
        }
    }

    private fun Project.configureJvmPostBuildTasks(
        kotlinTarget: KotlinJvmTarget,
        cargoBuildVariant: CargoJvmBuildVariant<*>,
        androidTarget: KotlinAndroidTarget?,
    ) {
        val buildTask = cargoBuildVariant.buildTaskProvider
        val resourcePrefix = cargoBuildVariant.build.resourcePrefix.orNull?.takeIf(String::isNotEmpty)
        val resourceDirectory = layout.buildDirectory
            .dir("intermediates/rust/${cargoBuildVariant.rustTarget.rustTriple}/${cargoBuildVariant.variant}")
        val copyDestination =
            if (resourcePrefix == null) resourceDirectory else resourceDirectory.map { it.dir(resourcePrefix) }
        val sourceLibraryFile = buildTask.flatMap { task ->
            task.libraryFileByCrateType.map { it[CrateType.SystemDynamicLibrary]!! }
        }

        val copyTask = tasks.register<Copy>({
            +"jvm"
            +cargoBuildVariant
        }) {
            group = TASK_GROUP
            from(sourceLibraryFile)
            into(copyDestination)
            dependsOn(buildTask)
        }

        if (cargoBuildVariant.build.jvm.get() && cargoBuildVariant.variant == cargoBuildVariant.build.jvmVariant.get()) {
            kotlinTarget.compilations.getByName("main").defaultSourceSet {
                resources.srcDir(resourceDirectory)
            }
            tasks.withType<ProcessResources> {
                if (name.contains(kotlinTarget.name)) {
                    dependsOn(copyTask)
                }
            }
        }

        if (androidTarget != null && cargoBuildVariant.build.androidUnitTest.get()) {
            androidExtension.sourceSets { sourceSets ->
                val testSourceSet = sourceSets.getByVariant("test", cargoBuildVariant.variant)
                testSourceSet.resources.srcDir(resourceDirectory)
            }

            // Copy the dynamic library to the current project's android unit tests.
            copyJvmBuildResultsToAndroidUnitTest(
                copyTask,
                cargoBuildVariant,
                resourceDirectory,
                sourceLibraryFile,
                resourcePrefix,
            )

            // Copy the dynamic library to all android unit tests of the same variant in projects dependent on this project.
            DependencyUtils.configureEachDependentProjects(project) { dependent ->
                dependent.copyJvmBuildResultsToAndroidUnitTest(
                    copyTask,
                    cargoBuildVariant,
                    resourceDirectory,
                    sourceLibraryFile,
                    resourcePrefix,
                )
            }
        }
    }

    private fun Project.copyJvmBuildResultsToAndroidUnitTest(
        copyTask: TaskProvider<Copy>,
        cargoBuildVariant: CargoJvmBuildVariant<*>,
        resourceDirectory: Provider<Directory>,
        sourceLibraryFile: Provider<RegularFile>,
        resourcePrefix: String?,
    ) {
        tasks.withType<ProcessJavaResTask> {
            if (name.contains("UnitTest") && cargoBuildVariant.variant == variant!!) {
                dependsOn(copyTask)
                // Override the default behavior of AGP excluding .so files, which causes UnsatisfiedLinkError
                // on Linux.
                from(
                    // Append a fileTree which only includes the Rust shared library.
                    fileTree(resourceDirectory).matching {
                        val fileName = sourceLibraryFile.get().asFile.name
                        it.includes += if (resourcePrefix == null) {
                            setOf("/$fileName")
                        } else {
                            setOf("/$resourcePrefix/$fileName")
                        }
                    }
                )
            }
        }
    }

    private fun Project.configureAndroidPostBuildTasks(
        cargoBuildVariant: CargoAndroidBuildVariant,
    ) {
        val buildTask = cargoBuildVariant.buildTaskProvider
        val findDynamicLibrariesTask = cargoBuildVariant.findNdkLibrariesTaskProvider
        findDynamicLibrariesTask.configure {
            it.libraryPathsCacheFile.set(it.outputCacheFile("libraryPathsCacheFile"))
            it.searchPaths.set(
                cargoBuildVariant.rustTarget.ndkLibraryDirectories(
                    sdkRoot = androidExtension.sdkDirectory,
                    apiLevel = androidMinSdk,
                    ndkVersion = androidNdkVersion,
                    ndkRoot = androidNdkRoot,
                )
            )
        }

        if (!cargoExtension.androidTargetsToBuild.get().contains(cargoBuildVariant.rustTarget))
            return

        val copyDestination =
            layout.buildDirectory.dir("intermediates/rust/${cargoBuildVariant.rustTarget.rustTriple}/${cargoBuildVariant.variant}")

        val copyTask = tasks.register<Copy>({
            +"android"
            +cargoBuildVariant
        }) {
            group = TASK_GROUP
            from(
                buildTask.flatMap { task -> task.libraryFileByCrateType.map { it[CrateType.SystemDynamicLibrary]!! } },
                findDynamicLibrariesTask.flatMap { it.libraryPaths },
            )
            into(copyDestination.map { it.dir(cargoBuildVariant.rustTarget.androidAbiName) })
            dependsOn(buildTask, findDynamicLibrariesTask)
        }

        tasks.withType<Jar> {
            if (name.lowercase().contains("android")) {
                if (cargoBuildVariant.variant == variant!!) {
                    inputs.dir(copyDestination)
                    dependsOn(copyTask)
                }
            }
        }

        tasks.withType<MergeSourceSetFolders> {
            if (name.lowercase().contains("jni")) {
                if (cargoBuildVariant.variant == variant!!) {
                    inputs.dir(copyDestination)
                    dependsOn(copyTask)
                }
            }
        }

        androidExtension.sourceSets { sourceSets ->
            val mainSourceSet = sourceSets.getByVariant(cargoBuildVariant.variant)
            mainSourceSet.jniLibs.srcDir(copyDestination)
        }
    }

    private fun Project.configureNativeCompilation(
        kotlinTarget: KotlinNativeTarget,
        cargoBuildVariant: CargoNativeBuildVariant<*>,
    ) {
        val buildTask = cargoBuildVariant.buildTaskProvider

        val buildOutputFile = buildTask
            .flatMap { it.libraryFileByCrateType }
            .map { it[CrateType.SystemStaticLibrary]!! }

        kotlinTarget.compilations.getByName("main") { compilation ->
            compilation.cinterops.register("rust") { cinterop ->
                cinterop.defFile(buildTask.flatMap { it.nativeStaticLibsDefFile })
                cinterop.extraOpts(
                    "-libraryPath",
                    cargoExtension.cargoPackage.zip(cargoBuildVariant.profile) { cargoPackage, profile ->
                        cargoPackage.outputDirectory(profile, cargoBuildVariant.rustTarget)
                    }.get()
                )
                project.tasks.named(cinterop.interopProcessingTaskName) { task ->
                    task.inputs.file(buildOutputFile)
                    task.dependsOn(buildTask)
                }
            }
            compilation.compilerOptions.configure {
                optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }

    private fun Project.configureCleanTasks() {
        val cleanCrate = tasks.register<CargoCleanTask>("cargoClean") {
            group = TASK_GROUP
            cargoPackage.set(cargoExtension.cargoPackage)
        }

        tasks.named<Delete>("clean") {
            dependsOn(cleanCrate)
        }
    }
}

private fun KotlinPlatformType.requiredCrateType(): CrateType? = when (this) {
    // TODO: properly handle JS and WASM targets
    KotlinPlatformType.common -> null
    KotlinPlatformType.jvm -> CrateType.SystemDynamicLibrary
    KotlinPlatformType.js -> CrateType.SystemDynamicLibrary
    KotlinPlatformType.androidJvm -> CrateType.SystemDynamicLibrary
    KotlinPlatformType.native -> CrateType.SystemStaticLibrary
    KotlinPlatformType.wasm -> CrateType.SystemStaticLibrary
}

private fun Task.outputCacheFile(propertyName: String): Provider<RegularFile> {
    val trimmedPropertyName = propertyName
        .substringBeforeLast("File")
        .substringBeforeLast("Cache")
    return project.layout.buildDirectory.file("taskOutputCache/$name/$trimmedPropertyName")
}
