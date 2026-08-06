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
import ch.ubique.uniffi.plugin.tasks.CopyNativeLibrariesTask
import ch.ubique.uniffi.plugin.tasks.GenerateDefFileTask
import ch.ubique.uniffi.plugin.tasks.GenerateDummyDefFileTask
import ch.ubique.uniffi.plugin.tasks.InstallBindgenTask
import ch.ubique.uniffi.plugin.tasks.MergeNativeLibrariesTask
import ch.ubique.uniffi.plugin.utils.NdkUtil
import ch.ubique.uniffi.plugin.utils.targetPackage
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File

@Suppress("UnstableApiUsage")
class UniffiPlugin : Plugin<Project> {
    companion object {
        private const val CINTEROP_NAME = "uniffi"
        private const val GENERATED_ROOT = "generated/uniffi"
        private const val CINTEROP_ROOT = "$GENERATED_ROOT/nativeInterop/cinterop"
    }

    private lateinit var uniffiExtension: UniffiExtension

    private lateinit var cargoExtension: CargoExtension

    /**
     * Which cargo profile the rust libraries are built with.
     *
     * Resolved once, at apply time, from `-Puniffi.profile` / `-PreleaseBuild`.
     * Both are read through [org.gradle.api.provider.ProviderFactory], so they are
     * tracked configuration-cache inputs — unlike the old implementation, which
     * sniffed `gradle.startParameter.taskNames` and therefore silently produced a
     * different task graph depending on what was typed on the command line.
     *
     * Kotlin/Native binaries do have a real DEBUG/RELEASE axis of their own, but the
     * cinterop klib (which embeds the rust static library) is per *compilation*, not
     * per binary, so it can only carry one profile. See the note in
     * [configureNativeTarget].
     */
    private var isRelease: Boolean = false

    /**
     * True while the IDE is importing the project.
     *
     * `idea.sync.active` is the signal the Kotlin Gradle plugin itself uses
     * (`org.jetbrains.kotlin.gradle.internal.isInIdeaSync`). Read through
     * `providers.systemProperty`, so it is a tracked configuration input.
     */
    private var isIdeSync: Boolean = false

    private lateinit var metadataJson: Provider<String>
    private lateinit var libraryName: Provider<String>
    private lateinit var packageName: Provider<String>
    private lateinit var cargoTargetDir: Provider<Directory>
    private lateinit var bindgenLibsDir: Provider<Directory>
    private lateinit var buildOutputDir: Provider<Directory>

    private lateinit var bindingsRootDir: Provider<Directory>

    /**
     * Applies the plugin to the target project.
     *
     * Everything is wired here, at apply time. Nothing is deferred to
     * `afterEvaluate` any more:
     *
     *  - AGP runs its `onVariants` callbacks inside *its* `afterEvaluate`, which is
     *    registered before ours, so anything deferred arrives too late.
     *  - `cargo metadata` is consumed as a `Provider`, so configuration (and every
     *    IDE sync) no longer shells out to cargo.
     *  - The set of Kotlin targets is read from a live collection
     *    (`targets.configureEach`) instead of being enumerated after evaluation.
     */
    override fun apply(project: Project) {
        // Register Extensions
        uniffiExtension = project.extensions.create("uniffi", UniffiExtension::class.java)
        cargoExtension = project.extensions.create("cargo", CargoExtension::class.java)

        // Current Configuration
        isRelease = project.providers.gradleProperty("uniffi.profile").map { it == "release" }
            .orElse(project.providers.gradleProperty("releaseBuild").map { it == "true" })
            .getOrElse(false)

        isIdeSync = project.providers.systemProperty("idea.sync.active")
            .map { it.toBoolean() }
            .getOrElse(false)
            && project.providers.gradleProperty("forceBuildNativeLibs")
                .map { it != "true" }
                .getOrElse(true)

        metadataJson = project.providers
            .of(CargoMetadataService::class.java) { spec ->
                spec.parameters.packageDirectory.set(cargoExtension.packageDirectory)
            }
        val metadata: Provider<CargoMetadata> = metadataJson.map(CargoMetadata::fromJsonString)

        libraryName = metadata.map { it.targetPackage.targets.first().name }
        packageName = metadata.map { it.targetPackage.name }
        cargoTargetDir = project.layout.dir(metadata.map { File(it.targetDirectory) })
        bindgenLibsDir = project.layout.buildDirectory.dir("bindgen-libs")
        buildOutputDir = project.layout.buildDirectory.dir("target")

        bindingsRootDir = project.layout.buildDirectory.dir(GENERATED_ROOT)

//        val namespace: Provider<String> = uniffiExtension.bindingsGeneration
//            .flatMap { it.namespace }
//            .orElse(libraryName)

        // Bingen & Bindings
        registerBingenTasks(project)


        // Configure Targets
        configureTargets(project)

        // Validation after configuration
        project.afterEvaluate { evaluated ->
            if (!evaluated.plugins.hasPlugin(Constants.Plugins.KMP_PLUGIN)) {
                throw GradleException("Kotlin Multiplatform Plugin is required!")
            }
            if (!uniffiExtension.bindingsGeneration.isPresent) {
                throw GradleException("Please call either 'generateFromLibrary' or 'generateFromUdl'.")
            }
        }
    }

    /**
     * Registers the [InstallBindgenTask], BuildLibraryForBindings ([CargoBuildTask]), and
     * [BuildBindingsTask]. Configures the task dependencies and hooks the [BuildBindingsTask] into
     * the build process.
     */
    private fun registerBingenTasks(project: Project): TaskProvider<BuildBindingsTask> {
        val installBindgenTask = project.tasks.register(Tasks.INSTALL_BINDGEN, InstallBindgenTask::class.java) { task ->
            task.source.set(uniffiExtension.bindgenSource)
            task.bindgenPath.set(project.layout.buildDirectory.dir("bindgen-install"))
            task.bindgenTmpPath.set(
                project.rootProject.layout.buildDirectory.dir("bindgen-install/target")
            )
        }

        val buildLibForBindingsTask = project.tasks.register(Tasks.BUILD_LIB_FOR_BINDINGS, CargoBuildTask::class.java) { task ->
            // Only if the bindings are generated from the library
            task.onlyIf {
                uniffiExtension.bindingsGeneration.orNull is BindingsGenerationFromLibrary
            }

            task.packageDirectory.set(cargoExtension.packageDirectory)
            task.release.set(false)
            task.packageName.set(packageName)
            task.libraryName.set(libraryName)
            task.cargoOutputDirectory.set(cargoTargetDir.map { it.dir("debug") })
            task.outputDirectory.set(bindgenLibsDir)
            task.useCross.set(false)
        }

        val buildBindingsTask = project.tasks.register(Tasks.BUILD_BINDINGS, BuildBindingsTask::class.java) { task ->
            task.dependsOn(installBindgenTask, buildLibForBindingsTask)

            task.packageDirectory.set(cargoExtension.packageDirectory)
            task.cargoMetadata.set(metadataJson)
            task.generateBindingsForExternalCrates.set(uniffiExtension.generateBindingsForExternalCrates)
            task.bindingsDirectory.set(bindingsRootDir)

            task.bindgen.set(
                project.layout.buildDirectory.file(
                    uniffiExtension.bindgenSource.map {
                        "bindgen-install/bin/${it.bindgenName ?: Constants.BINDGEN_BIN_NAME}"
                    }
                )
            )

            task.libraryFile.set(
                uniffiExtension.bindingsGeneration
                    .filter { it is BindingsGenerationFromLibrary }
                    .flatMap {
                        libraryName.flatMap { name ->
                            bindgenLibsDir.map { dir ->
                                dir.file(
                                    BuildTarget.RustTarget.forCurrentPlatform
                                        .dynamicLibraryName(name)
                                        ?: throw GradleException("Could not determine library file name!")
                                )
                            }
                        }
                    }
            )

            task.udlFile.set(
                uniffiExtension.bindingsGeneration
                    .filter { it is BindingsGenerationFromUdl }
                    .flatMap { (it as BindingsGenerationFromUdl).udlFile }
            )
        }

        // Run build bindings on sync
        project.pluginManager.withPlugin(Constants.Plugins.KMP_PLUGIN) {
            if ("prepareKotlinIdeaImport" in project.tasks.names) {
                project.tasks.named("prepareKotlinIdeaImport") { task ->
                    task.dependsOn(buildBindingsTask)
                }
            }
        }

        // Run build bindings on build
        project.tasks.withType(KotlinCompilationTask::class.java).configureEach { task ->
            task.dependsOn(buildBindingsTask)
        }
        project.tasks.withType(Jar::class.java).configureEach { task ->
            task.dependsOn(buildBindingsTask)
        }
        project.tasks.withType(CInteropProcess::class.java).configureEach { task ->
            task.dependsOn(buildBindingsTask)
        }

        return buildBindingsTask
    }

    /**
     * Configures the KMP build targets
     */
    private fun configureTargets(project: Project) {
        project.pluginManager.withPlugin(Constants.Plugins.KMP_PLUGIN) {
            val kmpExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            configureCommonMain(project, kmpExtension)

            kmpExtension.targets.configureEach { target ->
                val buildTarget = BuildTarget.fromTargetName(target.name) ?: return@configureEach

                registerRustTasksFor(project, buildTarget)

                when {
                    buildTarget == BuildTarget.Jvm ->
                        configureJvmTarget(project, kmpExtension)

                    buildTarget == BuildTarget.Android ->
                        configureAndroidSourceSets(project, kmpExtension)

                    target is KotlinNativeTarget ->
                        configureNativeTarget(
                            project,
                            buildTarget,
                            target,
                            kmpExtension,
                        )

                    else -> throw GradleException("Unknown target: $target")
                }
            }
        }

        project.pluginManager.withPlugin(Constants.Plugins.ANDROID_PLUGIN) {
            configureAndroidVariants(project)
        }
    }

    private fun configureCommonMain(project: Project, kmpExtension: KotlinMultiplatformExtension) {
        kmpExtension.sourceSets.named("commonMain") { sourceSet ->
            sourceSet.kotlin.srcDir(bindingsRootDir.map { it.dir("commonMain") })

            project.configurations.named(sourceSet.implementationConfigurationName) { configuration ->
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
    }

    private fun configureJvmTarget(project: Project, kmpExtension: KotlinMultiplatformExtension) {
        kmpExtension.sourceSets.named("jvmMain") { sourceSet ->
            sourceSet.kotlin.srcDir(bindingsRootDir.map { it.dir("jvmMain") })

            sourceSet.resources.srcDir(
                project.layout.buildDirectory.dir(
                    "intermediates/rust/jvmMain/resources/${Strings.release(isRelease)}"
                )
            )

            sourceSet.dependencies {
                implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
            }
        }

        project.tasks.named("jvmProcessResources") { task ->
            task.dependsOn(Tasks.copyNativeLibraries(BuildTarget.Jvm, isRelease, dynamic = true))
        }
    }

    private fun configureAndroidSourceSets(
        project: Project,
        kmpExtension: KotlinMultiplatformExtension,
    ) {
        kmpExtension.sourceSets.named("androidMain") { sourceSet ->
            sourceSet.kotlin.srcDir(project.layout.buildDirectory.dir("$GENERATED_ROOT/androidMain"))

            sourceSet.dependencies {
                implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}@aar")
            }
        }

        kmpExtension.sourceSets.configureEach { sourceSet ->
            if (sourceSet.name == "androidHostTest") {
                // Host tests run on the JVM, so they need the plain jar, not the aar.
                sourceSet.dependencies {
                    implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Native targets
    // ─────────────────────────────────────────────────────────────────────────

    private fun configureNativeTarget(
        project: Project,
        buildTarget: BuildTarget,
        nativeTarget: KotlinNativeTarget,
        kmpExtension: KotlinMultiplatformExtension,
    ) {
        kmpExtension.sourceSets.maybeCreate("nativeMain")
            .kotlin
            .srcDir(bindingsRootDir.map { it.dir("nativeMain") })

        val defFileTask = registerGenerateDefFileTask(project, buildTarget, libraryName)
        val dummyDefFileTask = registerGenerateDummyDefFileTask(project)

        // Native targets are architecture specific, so there is exactly one rust
        // target per build target.
        val rustTarget = buildTarget.checkedNativeTarget
        val dynamic = buildTarget.useDynamicLib == true

        val copyNativeLibsTaskName =
            Tasks.copyNativeLibraries(rustTarget, buildTarget, isRelease, dynamic)

        // Mirrors registerCopyNativeLibrariesTask's output layout for a rust target
        // without an abiName. Computed instead of read off the task, so the task is
        // not realized during configuration.
        val libraryIncludeDir = project.layout.buildDirectory
            .dir(
                "intermediates/rust/${buildTarget.sourceSetName}/resources/" +
                        "${Strings.release(isRelease)}/${rustTarget.jarLibraryPath}"
            )
            .get().asFile.path

        // Both def files now live at a fixed path. The old name embedded the crate's
        // library name, which is only known from `cargo metadata` — i.e. it forced a
        // blocking cargo invocation during configuration just to name a file. The
        // library name is still written *into* the file, at execution time.
        val defFile = project.layout.buildDirectory
            .file("$CINTEROP_ROOT/uniffi-${buildTarget.name}.def").get().asFile
        val dummyDefFile = project.layout.buildDirectory
            .file("$CINTEROP_ROOT/dummy.def").get().asFile

        nativeTarget.compilations.getByName("main") { compilation ->
            compilation.cinterops.register(CINTEROP_NAME) { cinterop ->
                cinterop.packageName("cinterop")

                if (isIdeSync) {
                    // During an import the headers are enough to produce a klib
                    cinterop.defFile(dummyDefFile)
                } else {
                    cinterop.defFile(defFile)

                    cinterop.extraOpts("-libraryPath", libraryIncludeDir)
                }

                project.tasks.named(cinterop.interopProcessingTaskName) { task ->
                    task.dependsOn(Tasks.BUILD_BINDINGS)

                    if (isIdeSync) {
                        task.dependsOn(dummyDefFileTask)
                    } else {
                        task.dependsOn(defFileTask)
                        task.dependsOn(copyNativeLibsTaskName)
                    }
                }
            }
        }

        nativeTarget.compilerOptions { options ->
            options.optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    private fun configureAndroidVariants(project: Project) {
        val androidComponents =
            project.extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)

        BuildTarget.RustTarget.entries.filter { it.isAndroid }.forEach { rustTarget ->
            val config = cargoExtension.compilations.getByName(rustTarget.name)

            val targetEnvironment: Provider<Map<String, String>> = androidComponents.sdkComponents
                .sdkDirectory
                .zip(cargoExtension.ndkVersion.orElse("")) { sdk, ndkVersion ->
                    NdkUtil.ndkEnvVariables(
                        sdkRoot = sdk.asFile,
                        // The KMP android extension's minSdk is not readable this early
                        // in the lifecycle; 21 matches the previous `?: 21` fallback.
                        apiLevel = 21,
                        ndkVersion = ndkVersion.takeIf(String::isNotEmpty),
                        ndkRoot = null,
                        rustTriple = rustTarget.rustTriple,
                        ndkLlvmTriple = rustTarget.ndkLlvmTriple,
                    )
                }

            listOf(true, false).forEach { release ->
                val taskName = Tasks.cargoBuild(rustTarget, release)
                if (taskName in project.tasks.names) {
                    project.tasks.named(taskName, CargoBuildTask::class.java) { task ->
                        // When cross is used it manages the environment itself.
                        task.additionalEnvironment.set(
                            config.useCross.flatMap { useCross ->
                                if (useCross) {
                                    project.provider { emptyMap() }
                                } else {
                                    targetEnvironment
                                }
                            }
                        )
                    }
                }
            }
        }

        // One task, one directory: that is what addGeneratedSourceDirectory wires.
        // The per-ABI copy tasks each own `<...>/jniLibs/<Profile>/<abi>`; this
        // regroups them under a single root that AGP assigns.
        val mergeJniLibsTask =
            project.tasks.register(
                Tasks.MERGE_ANDROID_JNI_LIBS,
                MergeNativeLibrariesTask::class.java,
            ) { task ->
                val rustTargets =
                    if (isRelease) BuildTarget.Android.releaseTargets else BuildTarget.Android.debugTargets

                rustTargets
                    // baseTargets (the host library, used by local tests) has no
                    // abiName and lands under resources/, not jniLibs/.
                    .filter { it.abiName != null }
                    .forEach { rustTarget ->
                        val copyTaskName = Tasks.copyNativeLibraries(
                            rustTarget, BuildTarget.Android, isRelease, dynamic = true
                        )
                        task.sourceDirectories.from(
                            project.tasks.named(copyTaskName, CopyNativeLibrariesTask::class.java)
                                .flatMap { it.outputDir }
                        )
                    }
            }

        // Local android tests run on the host JVM and load the library through JNA,
        // so they need the host build laid out exactly like the jvm target's resources.
        val mergeHostTestResourcesTask =
            project.tasks.register(
                Tasks.MERGE_ANDROID_TEST_RESOURCES,
                MergeNativeLibrariesTask::class.java,
            ) { task ->
                BuildTarget.Android.baseTargets.forEach { rustTarget ->
                    val copyTaskName = Tasks.copyNativeLibraries(
                        rustTarget, BuildTarget.Android, isRelease, dynamic = true
                    )
                    task.sourceDirectories.from(
                        project.tasks.named(copyTaskName, CopyNativeLibrariesTask::class.java)
                            .flatMap { it.outputDir }
                    )
                }
            }

        androidComponents.onVariants { variant ->
            // AGP 9's KMP library plugin produces exactly one variant ("androidMain")
            // and one aar, so there is no build type to read here — the profile comes
            // from `isRelease`. The old debug/release jniLibs source sets, and the
            // packageDebugResources / packageReleaseResources tasks the plugin used to
            // hook, no longer exist.
            variant.sources.jniLibs?.addGeneratedSourceDirectory(
                mergeJniLibsTask,
                MergeNativeLibrariesTask::outputDirectory,
            )

            // Empty unless the build script opts in with `withHostTest { }`.
            variant.hostTests.forEach { (_, hostTest) ->
                hostTest.sources.resources?.addGeneratedSourceDirectory(
                    mergeHostTestResourcesTask,
                    MergeNativeLibrariesTask::outputDirectory,
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Task registration
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers the cargo build and copy tasks needed by [buildTarget].
     *
     * Rust targets are shared between build targets (the host triple backs jvm, the
     * matching native target and android's local tests), hence [maybeRegister].
     */
    private fun registerRustTasksFor(project: Project, buildTarget: BuildTarget) {
        buildTarget.targets.forEach { rustTarget ->
            listOf(true, false).forEach { release ->
                registerCargoBuildTask(project, rustTarget, release, buildOutputDir)
            }
        }

        listOf(true, false).forEach { dynamic ->
            buildTarget.debugTargetsAll.forEach { rustTarget ->
                registerCopyNativeLibrariesTask(
                    project, buildTarget, rustTarget, release = false, dynamic = dynamic
                )
            }
            buildTarget.releaseTargetsAll.forEach { rustTarget ->
                registerCopyNativeLibrariesTask(
                    project, buildTarget, rustTarget, release = true, dynamic = dynamic
                )
            }

            // Umbrella tasks, so consumers can depend on "everything for this target".
            // TODO: `android.defaultConfig.ndk.abiFilters` used to narrow the release
            //       ABI list here. The new KMP android DSL has no equivalent, so this
            //       now always builds all three ABIs. If that matters, it needs a
            //       property on `cargo { }` instead.
            project.tasks.maybeRegister(
                Tasks.copyNativeLibraries(buildTarget, false, dynamic),
                Task::class.java,
            ) { task ->
                task.dependsOn(buildTarget.debugTargetsAll.map {
                    Tasks.copyNativeLibraries(it, buildTarget, false, dynamic)
                })
            }
            project.tasks.maybeRegister(
                Tasks.copyNativeLibraries(buildTarget, true, dynamic),
                Task::class.java,
            ) { task ->
                task.dependsOn(buildTarget.releaseTargetsAll.map {
                    Tasks.copyNativeLibraries(it, buildTarget, true, dynamic)
                })
            }
        }
    }

    private fun registerCargoBuildTask(
        project: Project,
        rustTarget: BuildTarget.RustTarget,
        release: Boolean,
        buildOutputDir: Provider<Directory>,
    ) {
        val profile = Strings.release(release).lowercase()

        project.tasks.maybeRegister(
            Tasks.cargoBuild(rustTarget, release),
            CargoBuildTask::class.java,
        ) { task ->
            task.packageDirectory.set(cargoExtension.packageDirectory)
            task.triple.set(rustTarget.rustTriple)
            task.release.set(release)
            task.packageName.set(packageName)
            task.libraryName.set(libraryName)
            task.cargoOutputDirectory.set(
                cargoTargetDir.map { it.dir("${rustTarget.rustTriple}/$profile") }
            )
            task.outputDirectory.set(
                buildOutputDir.map { it.dir("${rustTarget.rustTriple}/$profile") }
            )
            task.useCross.set(cargoExtension.compilations.getByName(rustTarget.name).useCross)
        }
    }

    private fun registerCopyNativeLibrariesTask(
        project: Project,
        buildTarget: BuildTarget,
        rustTarget: BuildTarget.RustTarget,
        release: Boolean,
        dynamic: Boolean,
    ) {
        val profile = Strings.release(release).lowercase()
        val sourceDir = buildOutputDir.map { it.dir("${rustTarget.rustTriple}/$profile") }

        // Android ABIs go to jniLibs/<Profile>/<abi>; everything else is laid out the
        // way JNA expects to find it on the classpath. MergeNativeLibrariesTask relies
        // on the last path segment being the abi / jarLibraryPath.
        val outputDirectory = if (rustTarget.abiName != null) {
            project.layout.buildDirectory.dir(
                "intermediates/rust/${buildTarget.sourceSetName}/jniLibs/" +
                        "${Strings.release(release)}/${rustTarget.abiName}"
            )
        } else {
            project.layout.buildDirectory.dir(
                "intermediates/rust/${buildTarget.sourceSetName}/resources/" +
                        "${Strings.release(release)}/${rustTarget.jarLibraryPath}"
            )
        }

        project.tasks.maybeRegister(
            Tasks.copyNativeLibraries(rustTarget, buildTarget, release, dynamic),
            CopyNativeLibrariesTask::class.java,
        ) { task ->
            task.libraryFile.set(
                libraryName.flatMap { name ->
                    val fileName = if (dynamic) {
                        rustTarget.dynamicLibraryName(name)
                    } else {
                        rustTarget.staticLibraryName(name)
                    } ?: throw GradleException("Could not determine library file name!")

                    sourceDir.map { it.file(fileName) }
                }
            )
            task.outputDir.set(outputDirectory)

            task.dependsOn(Tasks.cargoBuild(rustTarget, release))
        }
    }

    private fun registerGenerateDefFileTask(
        project: Project,
        buildTarget: BuildTarget,
        libraryName: Provider<String>,
    ): TaskProvider<GenerateDefFileTask> {
        val rustTarget = buildTarget.checkedNativeTarget
        val config = cargoExtension.compilations.getByName(rustTarget.name)

        return project.tasks.maybeRegister(
            Tasks.generateDefFile(buildTarget),
            GenerateDefFileTask::class.java,
        ) { task ->
            task.libraryName.set(
                libraryName.map {
                    rustTarget.staticLibraryName(it)
                        ?: throw GradleException("Could not determine library file name!")
                }
            )
            task.outputFile.set(
                project.layout.buildDirectory.file("$CINTEROP_ROOT/uniffi-${buildTarget.name}.def")
            )
            task.packageDirectory.set(cargoExtension.packageDirectory)
            task.targetString.set(rustTarget.rustTriple)
            task.headersDir.set(project.layout.buildDirectory.dir("$CINTEROP_ROOT/headers/"))
            task.useCross.set(config.useCross)

            task.dependsOn(Tasks.BUILD_BINDINGS)
        }
    }

    private fun registerGenerateDummyDefFileTask(project: Project): TaskProvider<GenerateDummyDefFileTask> =
        project.tasks.maybeRegister(
            Tasks.GENERATE_DUMMY_DEF,
            GenerateDummyDefFileTask::class.java,
        ) { task ->
            task.outputFile.set(project.layout.buildDirectory.file("$CINTEROP_ROOT/dummy.def"))
            task.headersDir.set(project.layout.buildDirectory.dir("$CINTEROP_ROOT/headers/"))

            task.dependsOn(Tasks.BUILD_BINDINGS)
        }

    /**
     * Registers [name] if it is not registered yet, otherwise returns the existing
     * provider without configuring it a second time.
     *
     * `names` does not realise tasks, so this stays lazy.
     */
    private fun <T : Task> TaskContainer.maybeRegister(
        name: String,
        type: Class<T>,
        configure: Action<T>,
    ): TaskProvider<T> =
        if (name in names) named(name, type) else register(name, type, configure)

    private fun DependencySet.addIf(condition: Provider<Boolean>, vararg dependencies: Dependency) =
        addAllLater(
            condition.map { enabled ->
                if (enabled) { dependencies.toList() } else { emptyList() }
            }
        )
}
