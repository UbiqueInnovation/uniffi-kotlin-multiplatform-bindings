package ch.ubique.uniffi.plugin

import ch.ubique.uniffi.plugin.dsl.BindingsGenerationFromLibrary
import ch.ubique.uniffi.plugin.dsl.BindingsGenerationFromUdl
import ch.ubique.uniffi.plugin.dsl.CargoExtension
import ch.ubique.uniffi.plugin.dsl.UniffiExtension
import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.model.CargoMetadata
import ch.ubique.uniffi.plugin.services.CargoMetadataService
import ch.ubique.uniffi.plugin.tasks.BuildBindingsTask
import ch.ubique.uniffi.plugin.tasks.CargoBuildTask
import ch.ubique.uniffi.plugin.tasks.InstallBindgenTask
import ch.ubique.uniffi.plugin.utils.targetPackage
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File

@Suppress("UnstableApiUsage")
class UniffiPlugin : Plugin<Project> {
    //    companion object {
//        private const val CINTEROP_NAME = "uniffi"
//        private const val GENERATED_ROOT = "generated/uniffi"
//        private const val CINTEROP_ROOT = "$GENERATED_ROOT/nativeInterop/cinterop"
//    }
//

    private companion object {
        private const val PREFIX: String = "uniffi"

        /** The path where the bindgen binary will be installed relative to the project */
        private const val BINDGEN_INSTALL_PATH: String = "$PREFIX/bindgen"

        /** The path where the bindgen binary will be built relative to the *root* project */
        private const val BINDGEN_BUILD_PATH: String = "$PREFIX/build/bindgen"

        /** The output directory of the bindgen */
        private const val BINDINGS_PATH: String = "$PREFIX/bindings"

        /** The directory where the built libraries will be copied to */
        private const val LIBS_OUTPUT_PATH: String = "$PREFIX/build/intermediates/libs"
    }

    private lateinit var uniffiExtension: UniffiExtension

    private lateinit var cargoExtension: CargoExtension
//
//    /**
//     * Which cargo profile the rust libraries are built with.
//     *
//     * Resolved from `-Puniffi.profile` / `-PreleaseBuild`.
//     */
//    private var isRelease: Boolean = false
//
//    /**
//     * The IDE sets `idea.sync.active` to true during a sync
//     */
//    private var isIdeSync: Boolean = false
//
//    private lateinit var metadataJson: Provider<String>
//    private lateinit var libraryName: Provider<String>
//    private lateinit var packageName: Provider<String>
//    private lateinit var cargoTargetDir: Provider<Directory>
//    private lateinit var bindgenLibsDir: Provider<Directory>
//    private lateinit var buildOutputDir: Provider<Directory>
//
//    private lateinit var bindingsRootDir: Provider<Directory>
//
//    /**
//     * The bindings task, kept so the generated source directories can be handed to the
//     * Kotlin source sets *as task outputs* rather than as bare paths. That is what makes
//     * the dependency implicit: see [bindingsSourceDir].
//     */
//    private lateinit var buildBindingsTask: TaskProvider<BuildBindingsTask>
//
//    /**
//     * The generated source directory for [sourceSetName], as a provider that carries a
//     * dependency on [buildBindingsTask].
//     *
//     * Handing this to `srcDir` means Gradle infers "compiling this source set requires
//     * buildBindings" on its own, which is what lets the blanket
//     * `withType(KotlinCompilationTask).dependsOn(buildBindings)` wiring go away.
//     */
//    private fun bindingsSourceDir(project: Project, sourceSetName: String): FileCollection =
//        project.files(buildBindingsTask.flatMap { it.bindingsDirectory.dir(sourceSetName) })
//            .builtBy(buildBindingsTask)

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

        val metadataJsonProvider = project.providers.of(CargoMetadataService::class.java) { spec ->
            spec.parameters.packageDirectory.set(cargoExtension.packageDirectory)
        }
        val metadata = metadataJsonProvider.map { CargoMetadata.fromJsonString(it) }
        val targetPackage = metadata.map { it.targetPackage }
        val packageName = targetPackage.map { it.name }
        val libraryName = targetPackage.map { it.targets.first().name }

        val installBindgenTask = project.registerInstallBindgenTask()
        val buildLibraryForBindingsTask = project.registerBuildLibraryForBindingsTask(
            packageName = packageName,
            libraryName = libraryName,
            targetDirectory = metadata.map { it.targetDirectory },
        )
        val libraryForBindings = buildLibraryForBindingsTask.flatMap {
            it.outputDirectory.file(it.libraryName.map { name ->
                BuildTarget.RustTarget.forCurrentPlatform.dynamicLibraryName(name)
                    ?: throw GradleException("Could not determine library file name!")
            })
        }
        val buildBindingsTask = project.registerBuildBindingsTask(
            bindgenBin = installBindgenTask.flatMap { it.bindgenBinPath },
            libraryForBindings = libraryForBindings,
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
                        librariesDir = buildBindingsTask.flatMap { it.jvmMainDir }, // TODO
                    )

                    BuildTarget.Android -> project.configureAndroidTarget(target)

                    in BuildTarget.nativeTargets -> project.configureNativeTarget(target as KotlinNativeTarget)

                    else -> throw GradleException("Unhandled target: $buildTarget")
                }
            }
        }

        project.afterEvaluate { evaluated ->
            if (!evaluated.plugins.hasPlugin(Constants.Plugins.KMP_PLUGIN)) {
                throw GradleException("Kotlin Multiplatform Plugin is required")
            }
        }
    }

    private fun Project.registerInstallBindgenTask(): TaskProvider<InstallBindgenTask> =
        project.tasks.register(Tasks.INSTALL_BINDGEN, InstallBindgenTask::class.java) { task ->
            task.source.set(uniffiExtension.bindgenSource)
            task.bindgenInstallPath.set(project.layout.buildDirectory.dir(BINDGEN_INSTALL_PATH))
            task.bindgenBuildPath.set(
                project.rootProject.layout.buildDirectory.dir(BINDGEN_BUILD_PATH)
            )
            task.defaultBindgenBinName.set(Constants.BINDGEN_BIN_NAME)
        }

    private fun Project.registerBuildLibraryForBindingsTask(
        packageName: Provider<String>,
        libraryName: Provider<String>,
        targetDirectory: Provider<String>,
    ): TaskProvider<CargoBuildTask> =
        project.tasks.register(Tasks.BUILD_LIB_FOR_BINDINGS, CargoBuildTask::class.java) { task ->
            task.packageDirectory.set(cargoExtension.packageDirectory)
            task.release.set(false)
            task.packageName.set(packageName)
            task.libraryName.set(libraryName)
            task.cargoOutputDirectory.set(
                project.layout.dir(targetDirectory.map { File(it, "debug") })
            )
            task.outputDirectory.set(
                project.layout.buildDirectory.dir(LIBS_OUTPUT_PATH)
            )
            task.useCross.set(false)
        }

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


//        // Register Extensions

    //
//        // Current Configuration
//        isRelease = project.providers.gradleProperty("uniffi.profile").map { it == "release" }
//            .orElse(project.providers.gradleProperty("releaseBuild").map { it == "true" })
//            .getOrElse(false)
//
//        isIdeSync = project.providers.systemProperty("idea.sync.active")
//            .map { it.toBoolean() }
//            .getOrElse(false)
//            && project.providers.gradleProperty("forceBuildNativeLibs")
//                .map { it != "true" }
//                .getOrElse(true)
//
//        metadataJson = project.providers
//            .of(CargoMetadataService::class.java) { spec ->
//                spec.parameters.packageDirectory.set(cargoExtension.packageDirectory)
//            }
//        val metadata: Provider<CargoMetadata> = metadataJson.map(CargoMetadata::fromJsonString)
//
//        libraryName = metadata.map { it.targetPackage.targets.first().name }
//        packageName = metadata.map { it.targetPackage.name }
//        cargoTargetDir = project.layout.dir(metadata.map { File(it.targetDirectory) })
//        bindgenLibsDir = project.layout.buildDirectory.dir("bindgen-libs")
//        buildOutputDir = project.layout.buildDirectory.dir("target")
//
//        bindingsRootDir = project.layout.buildDirectory.dir(GENERATED_ROOT)
//
////        val namespace: Provider<String> = uniffiExtension.bindingsGeneration
////            .flatMap { it.namespace }
////            .orElse(libraryName)
//
//        // Bingen & Bindings
//        buildBindingsTask = registerBingenTasks(project)
//
//
//        // Configure Targets
//        configureTargets(project)
//
//        // Validation after configuration
//        project.afterEvaluate { evaluated ->
//            if (!evaluated.plugins.hasPlugin(Constants.Plugins.KMP_PLUGIN)) {
//                throw GradleException("Kotlin Multiplatform Plugin is required!")
//            }
//            if (!uniffiExtension.bindingsGeneration.isPresent) {
//                throw GradleException("Please call either 'generateFromLibrary' or 'generateFromUdl'.")
//            }
//        }
//
//    /**
//     * Registers the [InstallBindgenTask], BuildLibraryForBindings ([CargoBuildTask]), and
//     * [BuildBindingsTask]. Configures the task dependencies and hooks the [BuildBindingsTask] into
//     * the build process.
//     */
//    private fun registerBingenTasks(project: Project): TaskProvider<BuildBindingsTask> {
//        val installBindgenTask = project.tasks.register(Tasks.INSTALL_BINDGEN, InstallBindgenTask::class.java) { task ->
//            task.source.set(uniffiExtension.bindgenSource)
//            task.bindgenPath.set(project.layout.buildDirectory.dir("bindgen-install"))
//            task.bindgenTmpPath.set(
//                project.rootProject.layout.buildDirectory.dir("bindgen-install/target")
//            )
//        }
//
//        // Captured by the onlyIf spec below, which Gradle serializes as part of the
//        // task's state. It has to be a plain Provider: a lambda that touches
//        // `uniffiExtension` would capture the UniffiPlugin instance instead, dragging
//        // every field it holds - including the buildBindings TaskProvider - into the
//        // configuration cache ("cannot serialize object of type BuildBindingsTask").
//        val generatesFromLibrary: Provider<Boolean> = uniffiExtension.bindingsGeneration
//            .map { it is BindingsGenerationFromLibrary }
//            .orElse(false)
//
//        val buildLibForBindingsTask = project.tasks.register(Tasks.BUILD_LIB_FOR_BINDINGS, CargoBuildTask::class.java) { task ->
//            // Only if the bindings are generated from the library
//            task.onlyIf { generatesFromLibrary.get() }
//
//            task.packageDirectory.set(cargoExtension.packageDirectory)
//            task.release.set(false)
//            task.packageName.set(packageName)
//            task.libraryName.set(libraryName)
//            task.cargoOutputDirectory.set(cargoTargetDir.map { it.dir("debug") })
//            task.outputDirectory.set(bindgenLibsDir)
//            task.useCross.set(false)
//        }
//
//        val buildBindingsTask = project.tasks.register(Tasks.BUILD_BINDINGS, BuildBindingsTask::class.java) { task ->
//            task.dependsOn(installBindgenTask, buildLibForBindingsTask)
//
//            task.packageDirectory.set(cargoExtension.packageDirectory)
//            task.cargoMetadata.set(metadataJson)
//            task.generateBindingsForExternalCrates.set(uniffiExtension.generateBindingsForExternalCrates)
//            task.bindingsDirectory.set(bindingsRootDir)
//
//            task.bindgen.set(
//                project.layout.buildDirectory.file(
//                    uniffiExtension.bindgenSource.map {
//                        "bindgen-install/bin/${it.bindgenName ?: Constants.BINDGEN_BIN_NAME}"
//                    }
//                )
//            )
//
//            task.libraryFile.set(
//                uniffiExtension.bindingsGeneration
//                    .filter { it is BindingsGenerationFromLibrary }
//                    .flatMap {
//                        libraryName.flatMap { name ->
//                            bindgenLibsDir.map { dir ->
//                                dir.file(
//                                    BuildTarget.RustTarget.forCurrentPlatform
//                                        .dynamicLibraryName(name)
//                                        ?: throw GradleException("Could not determine library file name!")
//                                )
//                            }
//                        }
//                    }
//            )
//
//            task.udlFile.set(
//                uniffiExtension.bindingsGeneration
//                    .filter { it is BindingsGenerationFromUdl }
//                    .flatMap { (it as BindingsGenerationFromUdl).udlFile }
//            )
//        }
//
//        // Run build bindings on sync
//        project.pluginManager.withPlugin(Constants.Plugins.KMP_PLUGIN) {
//            if ("prepareKotlinIdeaImport" in project.tasks.names) {
//                project.tasks.named("prepareKotlinIdeaImport") { task ->
//                    task.dependsOn(buildBindingsTask)
//                }
//            }
//        }
//
//        // NOTE: there is deliberately no blanket
//        //   withType(KotlinCompilationTask/Jar/CInteropProcess).dependsOn(buildBindings)
//        // any more. The generated sources are attached to the Kotlin source sets as
//        // outputs of this task (see bindingsSourceDir), so every compile, jar and
//        // cinterop that consumes them picks the dependency up on its own. The only
//        // consumer that still needs an explicit edge is the IDE import above, which does
//        // not necessarily realise a compilation.
//
//        return buildBindingsTask
//    }
//
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
//    private fun configureCommonMain(project: Project, kmpExtension: KotlinMultiplatformExtension) {
//        kmpExtension.sourceSets.named("commonMain") { sourceSet ->
//            sourceSet.kotlin.srcDir(bindingsSourceDir(project, "commonMain"))
//
//            project.configurations.named(sourceSet.implementationConfigurationName) { configuration ->
//                configuration.dependencies.addIf(
//                    condition = uniffiExtension.addRuntime,
//                    project.dependencies.create("ch.ubique.uniffi:runtime:${Constants.RUNTIME_VERSION}")
//                )
//
//                configuration.dependencies.addIf(
//                    condition = uniffiExtension.addDependencies,
//                    project.dependencies.create("com.squareup.okio:okio:${Constants.OKIO_VERSION}"),
//                    project.dependencies.create("org.jetbrains.kotlinx:atomicfu:${Constants.ATOMICFU_VERSION}"),
//                    project.dependencies.create("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Constants.COROUTINES_VERSION}"),
//                    project.dependencies.create("org.jetbrains.kotlinx:kotlinx-datetime:${Constants.DATETIME_VERSION}"),
//                )
//            }
//        }
//    }
//
//    private fun configureJvmTarget(project: Project, kmpExtension: KotlinMultiplatformExtension) {
//        // JNA looks the library up on the classpath under <jarLibraryPath>/, so all the
//        // per-triple copies have to sit under one root. Same shape as the android
//        // wiring: one merge task, consumed as a provider, so `jvmProcessResources` needs
//        // no explicit dependsOn.
//        val mergeResourcesTask = registerMergeNativeLibrariesTask(
//            project,
//            Tasks.MERGE_JVM_RESOURCES,
//            BuildTarget.Jvm,
//            project.layout.buildDirectory.dir("intermediates/rust/jvmMain/resources"),
//        )
//
//        kmpExtension.sourceSets.named("jvmMain") { sourceSet ->
//            sourceSet.kotlin.srcDir(bindingsSourceDir(project, "jvmMain"))
//
//            sourceSet.resources.srcDir(mergeResourcesTask.flatMap { it.outputDirectory })
//
//            sourceSet.dependencies {
//                implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
//            }
//        }
//    }
//
//    /**
//     * Registers a task that regroups the per-rust-target copies of [buildTarget] under a
//     * single root, keeping each copy's own directory name (the ABI, or JNA's
//     * `jarLibraryPath`) as the sub directory.
//     *
//     * [outputDirectory] is left unset when AGP assigns it through
//     * `addGeneratedSourceDirectory`.
//     */
//    private fun registerMergeNativeLibrariesTask(
//        project: Project,
//        taskName: String,
//        buildTarget: BuildTarget,
//        outputDirectory: Provider<Directory>? = null,
//        rustTargets: List<BuildTarget.RustTarget> = buildTarget.rustTargets(isRelease),
//    ): TaskProvider<MergeNativeLibrariesTask> =
//        project.tasks.register(taskName, MergeNativeLibrariesTask::class.java) { task ->
//            outputDirectory?.let(task.outputDirectory::set)
//
//            rustTargets.forEach { rustTarget ->
//                task.sourceDirectories.from(
//                    project.tasks
//                        .named(
//                            Tasks.copyNativeLibraries(rustTarget, buildTarget),
//                            CopyNativeLibrariesTask::class.java,
//                        )
//                        .flatMap { it.outputDir }
//                )
//            }
//        }
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
//
//    /**
//     * Registers [name] if it is not registered yet, otherwise returns the existing
//     * provider without configuring it a second time.
//     *
//     * `names` does not realise tasks, so this stays lazy.
//     */
//    private fun <T : Task> TaskContainer.maybeRegister(
//        name: String,
//        type: Class<T>,
//        configure: Action<T>,
//    ): TaskProvider<T> =
//        if (name in names) named(name, type) else register(name, type, configure)
//
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
