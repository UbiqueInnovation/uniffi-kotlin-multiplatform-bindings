package ch.ubique.uniffi.plugin

import ch.ubique.uniffi.plugin.dsl.BindingsGenerationFromLibrary
import ch.ubique.uniffi.plugin.dsl.BindingsGenerationFromUdl
import ch.ubique.uniffi.plugin.dsl.CargoExtension
import ch.ubique.uniffi.plugin.dsl.UniffiExtension
import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.model.CargoInfo
import ch.ubique.uniffi.plugin.model.CargoMetadata
import ch.ubique.uniffi.plugin.services.CargoMetadataService
import ch.ubique.uniffi.plugin.tasks.BuildBindingsTask
import ch.ubique.uniffi.plugin.tasks.CargoBuildTask
import ch.ubique.uniffi.plugin.tasks.InstallBindgenTask
import ch.ubique.uniffi.plugin.tasks.MergeLibrariesTask
import ch.ubique.uniffi.plugin.utils.targetPackage
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File

@Suppress("UnstableApiUsage")
class UniffiPlugin : Plugin<Project> {
    private companion object {
        private const val PREFIX: String = "uniffi"

        /** The path where the bindgen binary will be installed relative to the project */
        private const val BINDGEN_INSTALL_PATH: String = "$PREFIX/bindgen"

        /** The path where the bindgen binary will be built relative to the *root* project */
        private const val BINDGEN_BUILD_PATH: String = "$PREFIX/build/bindgen"

        /** The output directory of the bindgen */
        private const val BINDINGS_PATH: String = "$PREFIX/bindings"

        /** The root of the per rust target library copies */
        private const val RUST_LIBS_PATH: String = "$PREFIX/build/rust"

        /** The merged library directory of a source set, relative to the project */
        private fun librariesPath(sourceSetName: String): String =
            "$PREFIX/build/intermediates/$sourceSetName/libs"
    }

    private lateinit var uniffiExtension: UniffiExtension

    private lateinit var cargoExtension: CargoExtension

    override fun apply(project: Project) {
        // Create the DSL extensions
        uniffiExtension = project.extensions.create("uniffi", UniffiExtension::class.java)
        cargoExtension = project.extensions.create("cargo", CargoExtension::class.java)

        // Check if -PreleaseBuild=true is set
        val isRelease =
            project.providers.gradleProperty("releaseBuild").map { it == "true" }.getOrElse(false)
        // idea.sync.active is automatically set by any idea IDE
        val isSync = project.providers.systemProperty("idea.sync.active").map { it.toBoolean() }
            .getOrElse(false)

        // Collect the cargo metadata
        val metadataJsonProvider = project.providers.of(CargoMetadataService::class.java) { spec ->
            spec.parameters.packageDirectory.set(cargoExtension.packageDirectory)
        }
        val metadata = metadataJsonProvider.map { CargoMetadata.fromJsonString(it) }
        val targetPackage = metadata.map { it.targetPackage }
        val cargoInfo = CargoInfo(
            packageName = targetPackage.map { it.name },
            libraryName = targetPackage.map { it.targets.first().name },
            targetDirectory = project.layout.dir(metadata.map { File(it.targetDirectory) }),
        )

        // Set up the bindings tasks
        val installBindgenTask = project.registerInstallBindgenTask()
        val buildLibraryForBindingsTask = project.registerBuildLibraryForBindingsTask(cargoInfo)
        val buildBindingsTask = project.registerBuildBindingsTask(
            bindgenBin = installBindgenTask.flatMap { it.bindgenBinPath },
            libraryForBindings = buildLibraryForBindingsTask.flatMap { it.dynamicLibraryFile },
            metadataJson = metadataJsonProvider,
        )

        project.pluginManager.withPlugin(Constants.Plugins.KMP_PLUGIN) {
            val kmpExtension =
                project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            // Run build bindings on sync
            if ("prepareKotlinIdeaImport" in project.tasks.names) {
                project.tasks.named("prepareKotlinIdeaImport") { task ->
                    task.dependsOn(buildBindingsTask)
                }
            }

            val commonMain = kmpExtension.sourceSets.getByName("commonMain")
            project.configureCommonMain(commonMain, buildBindingsTask.flatMap { it.commonMainDir })

            kmpExtension.targets.configureEach { target ->
                val buildTarget = BuildTarget.fromTargetName(target.name) ?: return@configureEach

                when (buildTarget) {
                    BuildTarget.Jvm -> project.configureJvmTarget(
                        jvmMain = kmpExtension.sourceSets.maybeCreate("jvmMain"),
                        bindingsDir = buildBindingsTask.flatMap { it.jvmMainDir },
                        librariesDir = project.registerMergeLibrariesTask(
                            taskName = Tasks.MERGE_JVM_RESOURCES,
                            buildTarget = BuildTarget.Jvm,
                            cargoInfo = cargoInfo,
                            isRelease = isRelease,
                            outputDirectory = project.layout.buildDirectory.dir(
                                librariesPath(BuildTarget.Jvm.sourceSetName)
                            ),
                        ).flatMap { it.outputDirectory },
                    )

                    BuildTarget.Android -> project.configureAndroidTarget(target)

                    in BuildTarget.nativeTargets -> project.configureNativeTarget(target as KotlinNativeTarget)

                    else -> throw GradleException("Unhandled target: $buildTarget")
                }
            }
        }

        project.afterEvaluate { evaluated ->
            // Make sure the KMP Plugin is applied
            if (!evaluated.plugins.hasPlugin(Constants.Plugins.KMP_PLUGIN)) {
                throw GradleException("Kotlin Multiplatform Plugin is required")
            }

            // Make sure the binding generation source is specified
            if (!uniffiExtension.bindingsGeneration.isPresent) {
                throw GradleException("Please call either 'generateFromLibrary' or 'generateFromUdl'.")
            }
        }
    }

    /**
     * Installs the bindgen for the project in the relative path [BINDGEN_INSTALL_PATH]. Sets the
     * `CARGO_TARGET_DIR` to the path [BINDGEN_BUILD_PATH] relative to the **root project**, so that
     * cargo can reuse the build files if the root project has multiple subprojects where the
     * bindgen needs to be installed to.
     */
    private fun Project.registerInstallBindgenTask(): TaskProvider<InstallBindgenTask> =
        project.tasks.register(Tasks.INSTALL_BINDGEN, InstallBindgenTask::class.java) { task ->
            task.source.set(uniffiExtension.bindgenSource)
            task.bindgenInstallPath.set(project.layout.buildDirectory.dir(BINDGEN_INSTALL_PATH))
            task.bindgenBuildPath.set(
                project.rootProject.layout.buildDirectory.dir(BINDGEN_BUILD_PATH)
            )
            task.defaultBindgenBinName.set(Constants.BINDGEN_BIN_NAME)
        }

    /**
     * Register a [CargoBuildTask] for the host native target (by not setting the `rustTarget`).
     * This library will be used to generate the bindings from (in case the bindings are not
     * generated from a UDL file).
     *
     * NOTE: Maybe it would be worth to specify the `rustTarget` to [BuildTarget.RustTarget.forCurrentPlatform]
     * so that rust can reuse the built library. Alternatively, it would be nice if we could figure
     * out which libraries are already being built and use one of them for the bindings.
     */
    private fun Project.registerBuildLibraryForBindingsTask(
        cargoInfo: CargoInfo,
    ): TaskProvider<CargoBuildTask> =
        project.tasks.register(Tasks.BUILD_LIB_FOR_BINDINGS, CargoBuildTask::class.java) { task ->
            task.packageDirectory.set(cargoExtension.packageDirectory)
            task.release.set(false)
            task.packageName.set(cargoInfo.packageName)
            task.libraryName.set(cargoInfo.libraryName)
            task.cargoTargetDirectory.set(cargoInfo.targetDirectory)
            task.outputDirectory.set(
                project.layout.buildDirectory.dir("$RUST_LIBS_PATH/host")
            )
            task.useCross.set(false)
        }

    /**
     * Register the [BuildBindingsTask], the [libraryForBindings] will not be used if bindings are
     * generated from a UDL file.
     */
    private fun Project.registerBuildBindingsTask(
        bindgenBin: Provider<RegularFile>,
        libraryForBindings: Provider<RegularFile>,
        metadataJson: Provider<String>,
    ): TaskProvider<BuildBindingsTask> =
        tasks.register(Tasks.BUILD_BINDINGS, BuildBindingsTask::class.java) { task ->
            task.packageDirectory.set(cargoExtension.packageDirectory)
            task.cargoMetadata.set(metadataJson)
            task.generateBindingsForExternalCrates.set(
                uniffiExtension.generateBindingsForExternalCrates
            )
            task.bindingsDirectory.set(project.layout.buildDirectory.dir(BINDINGS_PATH))
            task.bindgen.set(bindgenBin)

            task.libraryFile.set(uniffiExtension.bindingsGeneration.filter { it is BindingsGenerationFromLibrary }
                .flatMap { libraryForBindings })
            task.udlFile.set(uniffiExtension.bindingsGeneration.filter { it is BindingsGenerationFromUdl }
                .flatMap { (it as BindingsGenerationFromUdl).udlFile })
        }

    /**
     * Register a [CargoBuildTask] for the [rustTarget] and [release].
     *
     * NOTE: Only ever one task for a combination of [rustTarget] and [release] is registered.
     */
    private fun Project.registerCargoBuildTask(
        rustTarget: BuildTarget.RustTarget,
        release: Boolean,
        cargoInfo: CargoInfo,
    ): TaskProvider<CargoBuildTask> =
        tasks.maybeRegister(
            Tasks.cargoBuild(rustTarget, release),
            CargoBuildTask::class.java,
        ) { task ->
            task.packageDirectory.set(cargoExtension.packageDirectory)
            task.rustTarget.set(rustTarget)
            task.release.set(release)
            task.packageName.set(cargoInfo.packageName)
            task.libraryName.set(cargoInfo.libraryName)
            task.cargoTargetDirectory.set(cargoInfo.targetDirectory)
            task.outputDirectory.set(
                layout.buildDirectory.dir(
                    "$RUST_LIBS_PATH/${rustTarget.rustTriple}/${Strings.release(release)}"
                )
            )
            task.useCross.set(cargoExtension.compilations.getByName(rustTarget.name).useCross)
        }

    /**
     * JVM / Android targets need multiple libraries to be in the correct directory structure.
     *
     * The [MergeLibrariesTask] combines outputs from multiple [CargoBuildTask]s and copies the
     * libraries into the correct directory structure.
     */
    private fun Project.registerMergeLibrariesTask(
        taskName: String,
        buildTarget: BuildTarget,
        cargoInfo: CargoInfo,
        isRelease: Boolean,
        rustTargets: List<BuildTarget.RustTarget> = buildTarget.rustTargets(isRelease),
        outputDirectory: Provider<Directory>? = null,
        leafName: (BuildTarget.RustTarget) -> String = { it.jarLibraryPath },
    ): TaskProvider<MergeLibrariesTask> {
        val cargoBuilds = rustTargets.map { rustTarget ->
            rustTarget to registerCargoBuildTask(rustTarget, isRelease, cargoInfo)
        }
        return tasks.register(taskName, MergeLibrariesTask::class.java) { task ->
            outputDirectory?.let(task.outputDirectory::set)

            cargoBuilds.forEach { (rustTarget, cargoBuild) ->
                task.library(
                    directoryName = leafName(rustTarget),
                    files = cargoBuild.flatMap {
                        if (buildTarget.usesDynamicLibrary) {
                            it.dynamicLibraryFile
                        } else {
                            it.staticLibraryFile
                        }
                    },
                )
            }
        }
    }

    private fun Project.configureCommonMain(
        commonMain: KotlinSourceSet,
        bindingsDir: Provider<Directory>,
    ) {
        commonMain.kotlin.srcDir(bindingsDir)

        configurations.named(commonMain.implementationConfigurationName) { configuration ->
            configuration.dependencies.addIf(
                condition = uniffiExtension.addRuntime,
                project.dependencies.create("ch.ubique.uniffi:runtime:${Constants.RUNTIME_VERSION}")
            )

            configuration.dependencies.addIf(
                condition = uniffiExtension.addDependencies,
                project.dependencies.create("com.squareup.okio:okio:${Constants.OKIO_VERSION}"),
                project.dependencies.create("org.jetbrains.kotlinx:atomicfu:${Constants.ATOMICFU_VERSION}"),
                project.dependencies.create("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Constants.COROUTINES_VERSION}"),
                project.dependencies.create("org.jetbrains.kotlinx:kotlinx-datetime:${Constants.DATETIME_VERSION}"),
            )
        }
    }

    private fun Project.configureJvmTarget(
        jvmMain: KotlinSourceSet,
        bindingsDir: Provider<Directory>,
        librariesDir: Provider<Directory>,
    ) {
        jvmMain.kotlin.srcDir(bindingsDir)

        jvmMain.resources.srcDir(librariesDir)

        jvmMain.dependencies {
            implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
        }
    }

    private fun Project.configureAndroidTarget(target: KotlinTarget) {
        // TODO
    }

    private fun Project.configureNativeTarget(target: KotlinNativeTarget) {
        // TODO
    }

//    /**
//     * Configures the KMP build targets
//     */
//    private fun configureTargets(project: Project) {
//        project.pluginManager.withPlugin(Constants.Plugins.KMP_PLUGIN) {
//            val kmpExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
//
//            configureCommonMain(project, kmpExtension)
//
//            kmpExtension.targets.configureEach { target ->
//                val buildTarget = BuildTarget.fromTargetName(target.name) ?: return@configureEach
//
//                registerRustTasksFor(project, buildTarget)
//
//                when {
//                    buildTarget == BuildTarget.Jvm ->
//                        configureJvmTarget(project, kmpExtension)
//
//                    buildTarget == BuildTarget.Android ->
//                        configureAndroidSourceSets(project, kmpExtension)
//
//                    target is KotlinNativeTarget ->
//                        configureNativeTarget(
//                            project,
//                            buildTarget,
//                            target,
//                            kmpExtension,
//                        )
//
//                    else -> throw GradleException("Unknown target: $target")
//                }
//            }
//        }
//
//        project.pluginManager.withPlugin(Constants.Plugins.ANDROID_PLUGIN) {
//            configureAndroidVariants(project)
//        }
//    }
//
//    private fun configureAndroidSourceSets(
//        project: Project,
//        kmpExtension: KotlinMultiplatformExtension,
//    ) {
//        kmpExtension.sourceSets.named("androidMain") { sourceSet ->
//            sourceSet.kotlin.srcDir(bindingsSourceDir(project, "androidMain"))
//
//            sourceSet.dependencies {
//                implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}@aar")
//            }
//        }
//
//        kmpExtension.sourceSets.configureEach { sourceSet ->
//            if (sourceSet.name == "androidHostTest") {
//                // Host tests run on the JVM, so they need the plain jar, not the aar.
//                sourceSet.dependencies {
//                    implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
//                }
//            }
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // Native targets
//    // ─────────────────────────────────────────────────────────────────────────
//
//    private fun configureNativeTarget(
//        project: Project,
//        buildTarget: BuildTarget,
//        nativeTarget: KotlinNativeTarget,
//        kmpExtension: KotlinMultiplatformExtension,
//    ) {
//        kmpExtension.sourceSets.maybeCreate("nativeMain")
//            .kotlin
//            .srcDir(bindingsSourceDir(project, "nativeMain"))
//
//        val defFileTask = registerGenerateDefFileTask(project, buildTarget, libraryName)
//        val dummyDefFileTask = registerGenerateDummyDefFileTask(project)
//
//        // Native targets are architecture specific, so there is exactly one rust
//        // target per build target.
//        val rustTarget = buildTarget.checkedNativeTarget
//
//        // A native target maps to exactly one rust target, so it consumes that copy task
//        // directly - no merging, and no umbrella task.
//        val copyNativeLibsTaskName = Tasks.copyNativeLibraries(rustTarget, buildTarget)
//
//        // cinterop's -libraryPath wants a plain string at configuration time, so this
//        // mirrors registerCopyNativeLibrariesTask's layout rather than reading it off the
//        // task (which would realize it during configuration).
//        val libraryIncludeDir = project.layout.buildDirectory
//            .dir("intermediates/rust/${buildTarget.sourceSetName}/libs/${rustTarget.jarLibraryPath}")
//            .get().asFile.path
//
//        // Both def files now live at a fixed path. The old name embedded the crate's
//        // library name, which is only known from `cargo metadata` — i.e. it forced a
//        // blocking cargo invocation during configuration just to name a file. The
//        // library name is still written *into* the file, at execution time.
//        val defFile = project.layout.buildDirectory
//            .file("$CINTEROP_ROOT/uniffi-${buildTarget.name}.def").get().asFile
//        val dummyDefFile = project.layout.buildDirectory
//            .file("$CINTEROP_ROOT/dummy.def").get().asFile
//
//        nativeTarget.compilations.getByName("main") { compilation ->
//            compilation.cinterops.register(CINTEROP_NAME) { cinterop ->
//                cinterop.packageName("cinterop")
//
//                if (isIdeSync) {
//                    // During an import the headers are enough to produce a klib
//                    cinterop.defFile(dummyDefFile)
//                } else {
//                    cinterop.defFile(defFile)
//
//                    cinterop.extraOpts("-libraryPath", libraryIncludeDir)
//                }
//
//                project.tasks.named(cinterop.interopProcessingTaskName) { task ->
//                    task.dependsOn(Tasks.BUILD_BINDINGS)
//
//                    if (isIdeSync) {
//                        task.dependsOn(dummyDefFileTask)
//                    } else {
//                        task.dependsOn(defFileTask)
//                        task.dependsOn(copyNativeLibsTaskName)
//                    }
//                }
//            }
//        }
//
//        nativeTarget.compilerOptions { options ->
//            options.optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
//        }
//    }
//
//    private fun configureAndroidVariants(project: Project) {
//        val androidComponents =
//            project.extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)
//
//        BuildTarget.RustTarget.entries.filter { it.isAndroid }.forEach { rustTarget ->
//            val config = cargoExtension.compilations.getByName(rustTarget.name)
//
//            val targetEnvironment: Provider<Map<String, String>> = androidComponents.sdkComponents
//                .sdkDirectory
//                .zip(cargoExtension.ndkVersion.orElse("")) { sdk, ndkVersion ->
//                    NdkUtil.ndkEnvVariables(
//                        sdkRoot = sdk.asFile,
//                        // The KMP android extension's minSdk is not readable this early
//                        // in the lifecycle; 21 matches the previous `?: 21` fallback.
//                        apiLevel = 21,
//                        ndkVersion = ndkVersion.takeIf(String::isNotEmpty),
//                        ndkRoot = null,
//                        rustTriple = rustTarget.rustTriple,
//                        ndkLlvmTriple = rustTarget.ndkLlvmTriple,
//                    )
//                }
//
//            listOf(true, false).forEach { release ->
//                val taskName = Tasks.cargoBuild(rustTarget, release)
//                if (taskName in project.tasks.names) {
//                    project.tasks.named(taskName, CargoBuildTask::class.java) { task ->
//                        // When cross is used it manages the environment itself.
//                        task.additionalEnvironment.set(
//                            config.useCross.flatMap { useCross ->
//                                if (useCross) {
//                                    project.provider { emptyMap() }
//                                } else {
//                                    targetEnvironment
//                                }
//                            }
//                        )
//                    }
//                }
//            }
//        }
//
//        // One task, one directory: that is what addGeneratedSourceDirectory wires. The
//        // output directory is left unset here because AGP assigns it.
//        val mergeJniLibsTask = registerMergeNativeLibrariesTask(
//            project,
//            Tasks.MERGE_ANDROID_JNI_LIBS,
//            BuildTarget.Android,
//            // Only the ABI targets belong in jniLibs; the host library (baseTargets) is
//            // for local tests and goes to resources instead.
//            rustTargets = BuildTarget.Android.rustTargets(isRelease).filter { it.abiName != null },
//        )
//
//        // Local android tests run on the host JVM and load the library through JNA, so
//        // they need the host build laid out exactly like the jvm target's resources.
//        val mergeHostTestResourcesTask = registerMergeNativeLibrariesTask(
//            project,
//            Tasks.MERGE_ANDROID_TEST_RESOURCES,
//            BuildTarget.Android,
//            rustTargets = BuildTarget.Android.baseTargets,
//        )
//
//        androidComponents.onVariants { variant ->
//            // AGP 9's KMP library plugin produces exactly one variant ("androidMain")
//            // and one aar, so there is no build type to read here — the profile comes
//            // from `isRelease`. The old debug/release jniLibs source sets, and the
//            // packageDebugResources / packageReleaseResources tasks the plugin used to
//            // hook, no longer exist.
//            variant.sources.jniLibs?.addGeneratedSourceDirectory(
//                mergeJniLibsTask,
//                MergeNativeLibrariesTask::outputDirectory,
//            )
//
//            // Empty unless the build script opts in with `withHostTest { }`.
//            variant.hostTests.forEach { (_, hostTest) ->
//                hostTest.sources.resources?.addGeneratedSourceDirectory(
//                    mergeHostTestResourcesTask,
//                    MergeNativeLibrariesTask::outputDirectory,
//                )
//            }
//        }
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // Task registration
//    // ─────────────────────────────────────────────────────────────────────────
//
//    /**
//     * Registers the cargo build and copy tasks needed by [buildTarget].
//     *
//     * Rust targets are shared between build targets (the host triple backs jvm, the
//     * matching native target and android's local tests), hence [maybeRegister].
//     */
//    /**
//     * Registers the rust tasks [buildTarget] needs: one cargo build per rust target, and
//     * one copy task per (rust target, build target).
//     *
//     * Previously this registered the full
//     * `rustTarget x profile x linkage (x umbrella)` cross product - 22 cargo tasks and 96
//     * copy tasks for :runtime, of which 9 and 12 were reachable. Two of those three axes
//     * were never real choices: the profile is one global build input, and the linkage
//     * follows from the build target.
//     *
//     * Rust targets are shared between build targets (the host triple backs jvm, the
//     * matching native target and android's local tests), hence [maybeRegister].
//     */
//    private fun registerRustTasksFor(project: Project, buildTarget: BuildTarget) {
//        buildTarget.rustTargets(isRelease).forEach { rustTarget ->
//            // The profile stays in the cargo task name, so it is visible from the task
//            // list whether a binary is built in debug or release.
//            registerCargoBuildTask(project, rustTarget, isRelease, buildOutputDir)
//            registerCopyNativeLibrariesTask(project, buildTarget, rustTarget)
//        }
//    }
//
//    private fun registerCargoBuildTask(
//        project: Project,
//        rustTarget: BuildTarget.RustTarget,
//        release: Boolean,
//        buildOutputDir: Provider<Directory>,
//    ) {
//        val profile = Strings.release(release).lowercase()
//
//        project.tasks.maybeRegister(
//            Tasks.cargoBuild(rustTarget, release),
//            CargoBuildTask::class.java,
//        ) { task ->
//            task.packageDirectory.set(cargoExtension.packageDirectory)
//            task.triple.set(rustTarget.rustTriple)
//            task.release.set(release)
//            task.packageName.set(packageName)
//            task.libraryName.set(libraryName)
//            task.cargoOutputDirectory.set(
//                cargoTargetDir.map { it.dir("${rustTarget.rustTriple}/$profile") }
//            )
//            task.outputDirectory.set(
//                buildOutputDir.map { it.dir("${rustTarget.rustTriple}/$profile") }
//            )
//            task.useCross.set(cargoExtension.compilations.getByName(rustTarget.name).useCross)
//        }
//    }
//
//    private fun registerCopyNativeLibrariesTask(
//        project: Project,
//        buildTarget: BuildTarget,
//        rustTarget: BuildTarget.RustTarget,
//    ) {
//        val profile = Strings.release(isRelease).lowercase()
//        val sourceDir = buildOutputDir.map { it.dir("${rustTarget.rustTriple}/$profile") }
//
//        // Each copy owns exactly one leaf directory, named after the ABI (android jniLibs)
//        // or after JNA's jarLibraryPath (everything else). The merge tasks rely on that
//        // name, and keeping the copies out of the merged roots avoids overlapping outputs.
//        val leafName = rustTarget.abiName ?: rustTarget.jarLibraryPath
//        val outputDirectory = project.layout.buildDirectory
//            .dir("intermediates/rust/${buildTarget.sourceSetName}/libs/$leafName")
//
//        project.tasks.maybeRegister(
//            Tasks.copyNativeLibraries(rustTarget, buildTarget),
//            CopyNativeLibrariesTask::class.java,
//        ) { task ->
//            task.libraryFile.set(
//                libraryName.flatMap { name ->
//                    val fileName = if (buildTarget.usesDynamicLibrary) {
//                        rustTarget.dynamicLibraryName(name)
//                    } else {
//                        rustTarget.staticLibraryName(name)
//                    } ?: throw GradleException("Could not determine library file name!")
//
//                    sourceDir.map { it.file(fileName) }
//                }
//            )
//            task.outputDir.set(outputDirectory)
//
//            task.dependsOn(Tasks.cargoBuild(rustTarget, isRelease))
//        }
//    }
//
//    private fun registerGenerateDefFileTask(
//        project: Project,
//        buildTarget: BuildTarget,
//        libraryName: Provider<String>,
//    ): TaskProvider<GenerateDefFileTask> {
//        val rustTarget = buildTarget.checkedNativeTarget
//        val config = cargoExtension.compilations.getByName(rustTarget.name)
//
//        return project.tasks.maybeRegister(
//            Tasks.generateDefFile(buildTarget),
//            GenerateDefFileTask::class.java,
//        ) { task ->
//            task.libraryName.set(
//                libraryName.map {
//                    rustTarget.staticLibraryName(it)
//                        ?: throw GradleException("Could not determine library file name!")
//                }
//            )
//            task.outputFile.set(
//                project.layout.buildDirectory.file("$CINTEROP_ROOT/uniffi-${buildTarget.name}.def")
//            )
//            task.packageDirectory.set(cargoExtension.packageDirectory)
//            task.targetString.set(rustTarget.rustTriple)
//            task.headersDir.set(project.layout.buildDirectory.dir("$CINTEROP_ROOT/headers/"))
//            task.useCross.set(config.useCross)
//
//            task.dependsOn(Tasks.BUILD_BINDINGS)
//        }
//    }
//
//    private fun registerGenerateDummyDefFileTask(project: Project): TaskProvider<GenerateDummyDefFileTask> =
//        project.tasks.maybeRegister(
//            Tasks.GENERATE_DUMMY_DEF,
//            GenerateDummyDefFileTask::class.java,
//        ) { task ->
//            task.outputFile.set(project.layout.buildDirectory.file("$CINTEROP_ROOT/dummy.def"))
//            task.headersDir.set(project.layout.buildDirectory.dir("$CINTEROP_ROOT/headers/"))
//
//            task.dependsOn(Tasks.BUILD_BINDINGS)
//        }

    /**
     * Registers [name] if it is not registered yet, otherwise returns the existing
     * provider without configuring it a second time.
     */
    private fun <T : Task> TaskContainer.maybeRegister(
        name: String,
        type: Class<T>,
        configure: Action<T>,
    ): TaskProvider<T> = if (name in names) named(name, type) else register(name, type, configure)

    private fun DependencySet.addIf(condition: Provider<Boolean>, vararg dependencies: Dependency) =
        addAllLater(
            condition.map { enabled ->
                if (enabled) {
                    dependencies.toList()
                } else {
                    emptyList()
                }
            })
}
