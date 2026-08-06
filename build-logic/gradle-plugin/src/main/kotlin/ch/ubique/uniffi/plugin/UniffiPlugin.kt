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
        private const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
        private const val ANDROID_KMP_LIBRARY_PLUGIN_ID = "com.android.kotlin.multiplatform.library"

        private const val INSTALL_BINDGEN_TASK_NAME = "installBindgen"
        private const val BUILD_LIB_FOR_BINDINGS_TASK_NAME = "buildLibraryForBindings"
        private const val BUILD_BINDINGS_TASK_NAME = "buildBindings"
        private const val GENERATE_DUMMY_DEF_FILE = "generateDummyDefFile"

        private const val MERGE_ANDROID_JNI_LIBS_TASK_NAME = "mergeUniffiAndroidJniLibs"
        private const val MERGE_ANDROID_TEST_RESOURCES_TASK_NAME = "mergeUniffiAndroidHostTestResources"

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
        // ─── 1. Extensions ────────────────────────────────────────────────────
        uniffiExtension = project.extensions.create("uniffi", UniffiExtension::class.java)
        cargoExtension = project.extensions.create("cargo", CargoExtension::class.java)

        // ─── 2. Tracked configuration inputs ──────────────────────────────────
        isRelease = project.providers.gradleProperty("uniffi.profile").map { it == "release" }
            .orElse(project.providers.gradleProperty("releaseBuild").map { it == "true" })
            .getOrElse(false)

        isIdeSync = project.providers.systemProperty("idea.sync.active")
            .map { it.toBoolean() }
            .getOrElse(false)
            && project.providers.gradleProperty("forceBuildNativeLibs")
                .map { it != "true" }
                .getOrElse(true)

        // ─── 3. Lazy facts. Nothing below calls .get() on these. ───────────────
        val metadataString: Provider<String> =
            project.providers.of(CargoMetadataService::class.java) { spec ->
                spec.parameters.packageDirectory.set(cargoExtension.packageDirectory)
            }
        val metadata: Provider<CargoMetadata> = metadataString.map(CargoMetadata::fromJsonString)

        val libraryName: Provider<String> = metadata.map { it.targetPackage.targets.first().name }
        val packageName: Provider<String> = metadata.map { it.targetPackage.name }
        val cargoTargetDir: Provider<Directory> =
            project.layout.dir(metadata.map { File(it.targetDirectory) })

        // The namespace default no longer needs a convention on a nested property
        // (which would require .get()ing `bindingsGeneration` before the build
        // script has run). `filter`/`orElse` expresses the same default lazily.
        @Suppress("UNUSED_VARIABLE")
        val namespace: Provider<String> = uniffiExtension.bindingsGeneration
            .flatMap { it.namespace }
            .orElse(libraryName)

        // ─── 4. Bindgen + bindings ────────────────────────────────────────────
        val bindgenLibsDir = project.layout.buildDirectory.dir("bindgen-libs")

        val installBindgenTask =
            project.tasks.register(INSTALL_BINDGEN_TASK_NAME, InstallBindgenTask::class.java) { task ->
                task.source.set(uniffiExtension.bindgenSource)
                task.bindgenPath.set(project.layout.buildDirectory.dir("bindgen-install"))
                task.bindgenTmpPath.set(
                    project.rootProject.layout.buildDirectory.dir("bindgen-install/target")
                )
            }

        // Only needed for `generateFromLibrary`; gated at execution time so the
        // decision does not have to be known while the task graph is being built.
        val buildBindingsLibTask =
            project.tasks.register(BUILD_LIB_FOR_BINDINGS_TASK_NAME, CargoBuildTask::class.java) { task ->
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

        val buildBindingsTask =
            project.tasks.register(BUILD_BINDINGS_TASK_NAME, BuildBindingsTask::class.java) { task ->
                task.dependsOn(installBindgenTask, buildBindingsLibTask)

                task.packageDirectory.set(cargoExtension.packageDirectory)
                task.cargoMetadata.set(metadataString)
                task.generateBindingsForExternalCrates.set(uniffiExtension.generateBindingsForExternalCrates)

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

        // ─── 5. Requirement: an IDE sync builds the bindings ──────────────────
        project.pluginManager.withPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID) {
            // KGP's supported "run before IDEA/Gradle import" umbrella. Replaces
            // hooking `commonize`, and with it the GradleException that used to
            // demand `kotlin.mpp.enableCInteropCommonization=true`.
            if ("prepareKotlinIdeaImport" in project.tasks.names) {
                project.tasks.named("prepareKotlinIdeaImport") { task ->
                    task.dependsOn(buildBindingsTask)
                }
            }
        }

        // ─── 6. Requirement: bindings exist before anything compiles ──────────
        project.tasks.withType(KotlinCompilationTask::class.java).configureEach { task ->
            task.dependsOn(buildBindingsTask)
        }
        project.tasks.withType(Jar::class.java).configureEach { task ->
            task.dependsOn(buildBindingsTask)
        }
        project.tasks.withType(CInteropProcess::class.java).configureEach { task ->
            task.dependsOn(buildBindingsTask)
        }

        // ─── 7. Per-target rust tasks and wiring ──────────────────────────────
        project.pluginManager.withPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID) {
            val kmpExtension = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            configureCommonMain(project, kmpExtension)

            // Live collection: fires as `kotlin { jvm(); iosArm64(); ... }` is
            // evaluated, so the target list never has to be read after evaluation.
            kmpExtension.targets.configureEach { target ->
                val buildTarget = BuildTarget.fromTargetName(target.name) ?: return@configureEach

                registerRustTasksFor(project, buildTarget, libraryName, packageName, cargoTargetDir)

                when (buildTarget) {
                    BuildTarget.Jvm ->
                        configureJvmTarget(project, kmpExtension)

                    BuildTarget.Android ->
                        configureAndroidSourceSets(project, kmpExtension)

                    else ->
                        configureNativeTarget(
                            project,
                            buildTarget,
                            target as KotlinNativeTarget,
                            kmpExtension,
                            libraryName,
                        )
                }
            }
        }

        // ─── 8. Android ───────────────────────────────────────────────────────
        // Registered at apply time on purpose: AGP fires onVariants from inside its
        // own afterEvaluate, so a deferred registration throws
        // "It is too late to add actions as the callbacks already executed".
        project.pluginManager.withPlugin(ANDROID_KMP_LIBRARY_PLUGIN_ID) {
            configureAndroidVariants(project, libraryName)
        }

        // ─── 9. Validation only — no wiring happens here ──────────────────────
        project.afterEvaluate { evaluated ->
            if (!evaluated.plugins.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID)) {
                throw GradleException("Kotlin Multiplatform Plugin is required!")
            }
            if (!uniffiExtension.bindingsGeneration.isPresent) {
                throw GradleException("Please call either 'generateFromLibrary' or 'generateFromUdl'.")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Source sets
    // ─────────────────────────────────────────────────────────────────────────

    private fun configureCommonMain(project: Project, kmpExtension: KotlinMultiplatformExtension) {
        kmpExtension.sourceSets.named("commonMain") { sourceSet ->
            sourceSet.kotlin.srcDir(project.layout.buildDirectory.dir("$GENERATED_ROOT/commonMain"))
        }

        // `addRuntime` / `addDependencies` are set from the build script's `uniffi { }`
        // block, which may be evaluated *after* commonMain is realised. Reading them
        // with .get() inside the source set configuration would therefore be a race
        // (the old code got away with it only because it ran in afterEvaluate).
        // addAllLater defers the read to dependency resolution.
        project.configurations.named("commonMainImplementation") { configuration ->
            configuration.dependencies.addAllLater(
                uniffiExtension.addRuntime.map { enabled ->
                    if (enabled) {
                        listOf(project.dependencies.create("ch.ubique.uniffi:runtime:${Constants.RUNTIME_VERSION}"))
                    } else {
                        emptyList()
                    }
                }
            )
            configuration.dependencies.addAllLater(
                uniffiExtension.addDependencies.map { enabled ->
                    if (enabled) {
                        listOf(
                            project.dependencies.create("com.squareup.okio:okio:${Constants.OKIO_VERSION}"),
                            project.dependencies.create("org.jetbrains.kotlinx:atomicfu:${Constants.ATOMICFU_VERSION}"),
                            project.dependencies.create("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Constants.COROUTINES_VERSION}"),
                            project.dependencies.create("org.jetbrains.kotlinx:kotlinx-datetime:${Constants.DATETIME_VERSION}"),
                        )
                    } else {
                        emptyList()
                    }
                }
            )
        }
    }

    private fun configureJvmTarget(project: Project, kmpExtension: KotlinMultiplatformExtension) {
        kmpExtension.sourceSets.named("jvmMain") { sourceSet ->
            sourceSet.kotlin.srcDir(project.layout.buildDirectory.dir("$GENERATED_ROOT/jvmMain"))

            sourceSet.resources.srcDir(
                project.layout.buildDirectory.dir(
                    "intermediates/rust/jvmMain/resources/${releaseString(isRelease)}"
                )
            )

            sourceSet.dependencies {
                implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
            }
        }

        project.tasks.named("jvmProcessResources") { task ->
            task.dependsOn(copyNativeLibrariesTaskName(BuildTarget.Jvm, isRelease, dynamic = true))
        }
    }

    private fun configureAndroidSourceSets(
        project: Project,
        kmpExtension: KotlinMultiplatformExtension,
    ) {
        kmpExtension.sourceSets.named("androidMain") { sourceSet ->
            sourceSet.kotlin.srcDir(project.layout.buildDirectory.dir("$GENERATED_ROOT/androidMain"))

            // The aar flavour of JNA, for code running on a device.
            sourceSet.dependencies {
                implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}@aar")
            }
        }

        // With `com.android.kotlin.multiplatform.library` the local-test source set
        // is called `androidHostTest`, and it only exists when the build script opts
        // in with `withHostTest { }`. The old code did `maybeCreate("androidUnitTest")`,
        // which silently created a source set attached to no compilation
        // ("The Kotlin source set androidUnitTest was configured but not added to any
        // Kotlin compilation"). configureEach only fires if the source set is real.
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
        libraryName: Provider<String>,
    ) {
        kmpExtension.sourceSets.maybeCreate("nativeMain")
            .kotlin
            .srcDir(project.layout.buildDirectory.dir("$GENERATED_ROOT/nativeMain"))

        val defFileTask = registerGenerateDefFileTask(project, buildTarget, libraryName)
        val dummyDefFileTask = registerGenerateDummyDefFileTask(project)

        // Native targets are architecture specific, so there is exactly one rust
        // target per build target.
        val rustTarget = buildTarget.checkedNativeTarget
        val dynamic = buildTarget.useDynamicLib == true

        val copyNativeLibsTaskName =
            copyNativeLibrariesTaskName(rustTarget, buildTarget, isRelease, dynamic)


        // Mirrors registerCopyNativeLibrariesTask's output layout for a rust target
        // without an abiName. Computed instead of read off the task, so the task is
        // not realised during configuration.
        val libraryIncludeDir = project.layout.buildDirectory
            .dir(
                "intermediates/rust/${buildTarget.sourceSetName}/resources/" +
                        "${releaseString(isRelease)}/${rustTarget.jarLibraryPath}"
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
                    // During an import the headers are enough to produce a klib; the
                    // rust static library is not needed, so the sync does not have to
                    // wait for cargo. This is the same trade-off the previous code
                    // made, but keyed off `idea.sync.active` rather than an empty
                    // startParameter.taskNames.
                    cinterop.defFile(dummyDefFile)
                } else {
                    cinterop.defFile(defFile)

                    cinterop.extraOpts("-libraryPath", libraryIncludeDir)
                }

                project.tasks.named(cinterop.interopProcessingTaskName) { task ->
                    task.dependsOn(BUILD_BINDINGS_TASK_NAME)

                    if (isIdeSync) {
                        task.dependsOn(dummyDefFileTask)
                    } else {
                        task.dependsOn(defFileTask)
                        task.dependsOn(copyNativeLibsTaskName)
                    }
                }
            }
        }

        // NOTE (follow-up, deliberately not done here): Kotlin/Native binaries carry
        // their own NativeBuildType, so debug/release could be decided per binary
        // rather than from `isRelease`. That only works if the rust static library is
        // supplied at *link* time (binary.linkerOpts) instead of being embedded by
        // cinterop via `staticLibraries`. Moving it changes what ends up inside
        // `framework { isStatic = true }` outputs, so it needs to be validated
        // against a real iOS consumer before being switched over.

        nativeTarget.compilerOptions { options ->
            options.optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Android variants
    // ─────────────────────────────────────────────────────────────────────────

    private fun configureAndroidVariants(project: Project, libraryName: Provider<String>) {
        val androidComponents =
            project.extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)

        // `KotlinMultiplatformAndroidLibraryExtension` has no sdkDirectory / ndkPath /
        // ndkVersion / defaultConfig.ndk.abiFilters — all of that lived on the removed
        // `com.android.build.gradle.BaseExtension`. SdkComponents is the replacement
        // for the two paths; there is no replacement for ndkVersion or abiFilters.
        // Built per rust target, because CARGO_TARGET_<TRIPLE>_LINKER / CC_<triple> /
        // AR_<triple> / ... are all triple specific.
        BuildTarget.RustTarget.entries.filter { it.isAndroid }.forEach { rustTarget ->
            val config = cargoExtension.compilations.getByName(rustTarget.name)

            // Only sdkDirectory is used. sdkComponents.ndkDirectory throws
            // "NDK is not installed" unless the AGP-default NDK version happens to be
            // present, and the new KMP DSL offers no ndkVersion to steer it with.
            // NdkUtil already resolves <sdk>/ndk/<newest> and falls back to
            // $ANDROID_NDK_ROOT, which is what the old code relied on too (ndkPath was
            // almost always null).
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
                val taskName = cargoBuildTaskName(rustTarget, release)
                if (taskName in project.tasks.names) {
                    project.tasks.named(taskName, CargoBuildTask::class.java) { task ->
                        // When cross is used it manages the environment itself.
                        task.additionalEnvironment.set(
                            config.useCross.flatMap { useCross ->
                                if (useCross) {
                                    project.provider { emptyMap<String, String>() }
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
                MERGE_ANDROID_JNI_LIBS_TASK_NAME,
                MergeNativeLibrariesTask::class.java,
            ) { task ->
                val rustTargets =
                    if (isRelease) BuildTarget.Android.releaseTargets else BuildTarget.Android.debugTargets

                rustTargets
                    // baseTargets (the host library, used by local tests) has no
                    // abiName and lands under resources/, not jniLibs/.
                    .filter { it.abiName != null }
                    .forEach { rustTarget ->
                        val copyTaskName = copyNativeLibrariesTaskName(
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
                MERGE_ANDROID_TEST_RESOURCES_TASK_NAME,
                MergeNativeLibrariesTask::class.java,
            ) { task ->
                BuildTarget.Android.baseTargets.forEach { rustTarget ->
                    val copyTaskName = copyNativeLibrariesTaskName(
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
     * Called from `targets.configureEach`, so only targets the build script actually
     * declares get tasks — the old code registered the full
     * `BuildTarget × RustTarget × release × dynamic` cross product and then realised
     * every one of them with `named { }`.
     *
     * Rust targets are shared between build targets (the host triple backs jvm, the
     * matching native target and android's local tests), hence [maybeRegister].
     */
    private fun registerRustTasksFor(
        project: Project,
        buildTarget: BuildTarget,
        libraryName: Provider<String>,
        packageName: Provider<String>,
        cargoTargetDir: Provider<Directory>,
    ) {
        val buildOutputDir = project.layout.buildDirectory.dir("target")

        buildTarget.targets.forEach { rustTarget ->
            listOf(true, false).forEach { release ->
                registerCargoBuildTask(
                    project, rustTarget, release, libraryName, packageName, cargoTargetDir, buildOutputDir
                )
            }
        }

        listOf(true, false).forEach { dynamic ->
            buildTarget.debugTargetsAll.forEach { rustTarget ->
                registerCopyNativeLibrariesTask(
                    project, buildTarget, rustTarget, release = false, dynamic = dynamic,
                    libraryName = libraryName, buildOutputDir = buildOutputDir,
                )
            }
            buildTarget.releaseTargetsAll.forEach { rustTarget ->
                registerCopyNativeLibrariesTask(
                    project, buildTarget, rustTarget, release = true, dynamic = dynamic,
                    libraryName = libraryName, buildOutputDir = buildOutputDir,
                )
            }

            // Umbrella tasks, so consumers can depend on "everything for this target".
            // TODO: `android.defaultConfig.ndk.abiFilters` used to narrow the release
            //       ABI list here. The new KMP android DSL has no equivalent, so this
            //       now always builds all three ABIs. If that matters, it needs a
            //       property on `cargo { }` instead.
            project.tasks.maybeRegister(
                copyNativeLibrariesTaskName(buildTarget, false, dynamic),
                Task::class.java,
            ) { task ->
                task.dependsOn(buildTarget.debugTargetsAll.map {
                    copyNativeLibrariesTaskName(it, buildTarget, false, dynamic)
                })
            }
            project.tasks.maybeRegister(
                copyNativeLibrariesTaskName(buildTarget, true, dynamic),
                Task::class.java,
            ) { task ->
                task.dependsOn(buildTarget.releaseTargetsAll.map {
                    copyNativeLibrariesTaskName(it, buildTarget, true, dynamic)
                })
            }
        }
    }

    private fun registerCargoBuildTask(
        project: Project,
        rustTarget: BuildTarget.RustTarget,
        release: Boolean,
        libraryName: Provider<String>,
        packageName: Provider<String>,
        cargoTargetDir: Provider<Directory>,
        buildOutputDir: Provider<Directory>,
    ) {
        val profile = cargoProfileDirectory(release)

        project.tasks.maybeRegister(
            cargoBuildTaskName(rustTarget, release),
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
        libraryName: Provider<String>,
        buildOutputDir: Provider<Directory>,
    ) {
        val profile = cargoProfileDirectory(release)
        val sourceDir = buildOutputDir.map { it.dir("${rustTarget.rustTriple}/$profile") }

        // Android ABIs go to jniLibs/<Profile>/<abi>; everything else is laid out the
        // way JNA expects to find it on the classpath. MergeNativeLibrariesTask relies
        // on the last path segment being the abi / jarLibraryPath.
        val outputDirectory = if (rustTarget.abiName != null) {
            project.layout.buildDirectory.dir(
                "intermediates/rust/${buildTarget.sourceSetName}/jniLibs/" +
                        "${releaseString(release)}/${rustTarget.abiName}"
            )
        } else {
            project.layout.buildDirectory.dir(
                "intermediates/rust/${buildTarget.sourceSetName}/resources/" +
                        "${releaseString(release)}/${rustTarget.jarLibraryPath}"
            )
        }

        project.tasks.maybeRegister(
            copyNativeLibrariesTaskName(rustTarget, buildTarget, release, dynamic),
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

            task.dependsOn(cargoBuildTaskName(rustTarget, release))
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
            generateDefFileTaskName(buildTarget),
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

            task.dependsOn(BUILD_BINDINGS_TASK_NAME)
        }
    }

    private fun registerGenerateDummyDefFileTask(project: Project): TaskProvider<GenerateDummyDefFileTask> =
        project.tasks.maybeRegister(
            GENERATE_DUMMY_DEF_FILE,
            GenerateDummyDefFileTask::class.java,
        ) { task ->
            task.outputFile.set(project.layout.buildDirectory.file("$CINTEROP_ROOT/dummy.def"))
            task.headersDir.set(project.layout.buildDirectory.dir("$CINTEROP_ROOT/headers/"))

            task.dependsOn(BUILD_BINDINGS_TASK_NAME)
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

    // ─────────────────────────────────────────────────────────────────────────
    // Naming helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun releaseString(release: Boolean) = if (release) {
        "Release"
    } else {
        "Debug"
    }

    /** The directory cargo itself writes into: `target/<triple>/{debug,release}`. */
    private fun cargoProfileDirectory(release: Boolean) = if (release) {
        "release"
    } else {
        "debug"
    }

    private fun dynamicString(dynamic: Boolean) = if (dynamic) {
        "Dynamic"
    } else {
        "Static"
    }

    private fun cargoBuildTaskName(
        target: BuildTarget.RustTarget,
        release: Boolean
    ): String = "cargoBuild${target.name}${releaseString(release)}"

    private fun copyNativeLibrariesTaskName(
        rustTarget: BuildTarget.RustTarget,
        buildTarget: BuildTarget,
        release: Boolean,
        dynamic: Boolean,
    ): String =
        "copyNative${dynamicString(dynamic)}Libs${rustTarget.name}${releaseString(release)}For${buildTarget.name}"

    private fun copyNativeLibrariesTaskName(
        buildTarget: BuildTarget,
        release: Boolean,
        dynamic: Boolean,
    ): String =
        "copyNative${dynamicString(dynamic)}Libs${releaseString(release)}${buildTarget.name}"

    private fun generateDefFileTaskName(
        buildTarget: BuildTarget
    ): String =
        "generateDefFileFor${buildTarget.name}"

    // ═════════════════════════════════════════════════════════════════════════
    // Previous implementation, kept for reference during the AGP 9 / Gradle 9
    // migration. Everything below ran from a single `project.afterEvaluate`.
    // ═════════════════════════════════════════════════════════════════════════

//    private lateinit var cargoMetadata: CargoMetadata
//    private val targetPackage: CargoMetadata.Package
//        get() = cargoMetadata.targetPackage
//
//    private val libraryName: String
//        get() = targetPackage.targets.first().name
//
//    private lateinit var buildVariant: CargoBuildVariant
//
//    private val isRelease: Boolean
//        get() = buildVariant == CargoBuildVariant.Release
//
//    override fun apply(project: Project) {
//        // Create the extensions
//        uniffiExtension = project.extensions.create("uniffi", UniffiExtension::class.java)
//        cargoExtension = project.extensions.create("cargo", CargoExtension::class.java)
//
//        // Register tasks
//        registerBindgenTasks(project)
//
//        registerBuildTasks(project)
//
//        registerGenerateDefFileTask(project)
//
//        // Configure tasks after evaluation
//        project.afterEvaluate { afterEvaluate(this) }
//    }
//
//    private fun doAndroidStuff(project: Project) {
//        val androidComponents = project.extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)
//
//        androidComponents.onVariants { variant ->
//            // 1. Get your Copy task for the main release build
//            val copyReleaseLibsTask = project.tasks.named(
//                copyNativeLibrariesTaskName(BuildTarget.Android, release = true, dynamic = true),
//                CopyNativeLibrariesTask::class.java
//            )
//
//            // 2. Wire it directly to the JNI sources
//            variant.sources.jniLibs?.addGeneratedSourceDirectory(
//                taskProvider = copyReleaseLibsTask,
//                wiredWith = CopyNativeLibrariesTask::outputDir
//            )
//
//            // 3. (Optional) If you have device tests, wire the debug build to them
//            val copyDebugLibsTask = project.tasks.named(
//                copyNativeLibrariesTaskName(BuildTarget.Android, release = false, dynamic = true),
//                CopyNativeLibrariesTask::class.java
//            )
//
//            variant.deviceTests.forEach { (_, testComponent) ->
//                testComponent.sources.jniLibs?.addGeneratedSourceDirectory(
//                    taskProvider = copyDebugLibsTask,
//                    wiredWith = CopyNativeLibrariesTask::outputDir
//                )
//            }
//        }
//    }
//
//    /**
//     * Runs after the project was evaluated. Checks whether all configuration is
//     * as expected and configures the tasks accordingly.
//     */
//    private fun afterEvaluate(project: Project) {
//        // Check if the KMP Plugin is applied
//        if (!project.plugins.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID)) {
//            throw GradleException("Kotlin Multiplatform Plugin is required!")
//        }
//
//        // Make sure the CInteropProcess Task will run
//        if (project.tasks.find { it.name == "commonize" } == null) {
//            throw GradleException("Please set 'kotlin.mpp.enableCInteropCommonization=true' in gradle.properties")
//        }
//
//        // Make sure the bindings generation is defined
//        if (!uniffiExtension.bindingsGeneration.isPresent) {
//            throw GradleException("Please call either 'generateFromLibrary' or 'generateFromUdl'.")
//        }
//
//        val cargoMetadataService = project.providers.of(CargoMetadataService::class.java) {
//            parameters.packageDirectory.set(cargoExtension.packageDirectory)
//        }
//
//        cargoMetadata = CargoMetadata.fromJsonString(cargoMetadataService.get())
//
//        uniffiExtension.bindingsGeneration.get().namespace.convention(libraryName)
//
//        configureBindgenTasks(project, cargoMetadataService)
//
//        configureBuildTasks(
//            project,
//            cargoExtension.packageDirectory
//        )
//
//        // The CInteropProcess Task will run on Sync if
//        // 'kotlin.mpp.enableCInteropCommonization' is
//        // set to true.
//        // Use it to hook into the Sync process and
//        // build the bindings.
//        project.tasks.named("commonize") {
//            dependsOn(BUILD_BINDINGS_TASK_NAME)
//        }
//
//        // Build the debug variant if unsure, the release
//        // variant has to be explicitly requested.
//        buildVariant = currentBuildVariant(project)
//            ?: CargoBuildVariant.Debug
//
//        // Configure the different targets
//        project.plugins.withId(KOTLIN_MULTIPLATFORM_PLUGIN_ID) {
//            val kotlinExt = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
//
//            configureTargets(project, kotlinExt)
//        }
//
//        // Make sure the bindings are built before kotlin code is compiled
//        project.tasks.withType<KotlinCompilationTask<*>> {
//            dependsOn(BUILD_BINDINGS_TASK_NAME)
//        }
//        project.tasks.withType<Jar> {
//            dependsOn(BUILD_BINDINGS_TASK_NAME)
//        }
//        project.tasks.withType<CInteropProcess> {
//            dependsOn(BUILD_BINDINGS_TASK_NAME)
//        }
//    }
//
//    /**
//     * Registers the InstallBindgen and BuildBindings Task
//     */
//    private fun registerBindgenTasks(project: Project) {
//        project.tasks.register<InstallBindgenTask>(INSTALL_BINDGEN_TASK_NAME)
//
//        project.tasks.register<CargoBuildTask>(BUILD_LIB_FOR_BINDINGS_TASK_NAME)
//
//        project.tasks.register<BuildBindingsTask>(BUILD_BINDINGS_TASK_NAME)
//    }
//
//    /**
//     * Configures the InstallBindgen and BuildBindings Task
//     */
//    private fun configureBindgenTasks(
//        project: Project,
//        cargoMetadataProvider: Provider<String>
//    ) {
//        project.tasks.named<InstallBindgenTask>(INSTALL_BINDGEN_TASK_NAME) {
//            bindgenPath.set(project.layout.buildDirectory.dir("bindgen-install"))
//            bindgenTmpPath.set(project.rootProject.layout.buildDirectory.dir("bindgen-install/target"))
//            source.set(uniffiExtension.bindgenSource)
//        }
//
//        val bindgenName =
//            uniffiExtension.bindgenSource.get().bindgenName ?: Constants.BINDGEN_BIN_NAME
//
//        val cargoOutputDir =
//            project.objects.directoryProperty().fileValue(File(cargoMetadata.targetDirectory))
//                .dir("debug")
//
//        val generation = uniffiExtension.bindingsGeneration.get()
//
//        when(generation) {
//            is BindingsGenerationFromLibrary -> {
//                val outputDir = project.layout.buildDirectory.dir("bindgen-libs")
//
//                project.tasks.named<CargoBuildTask>(BUILD_LIB_FOR_BINDINGS_TASK_NAME) {
//                    this.packageDirectory.set(cargoExtension.packageDirectory)
//                    this.release.set(false)
//                    this.packageName.set(targetPackage.name)
//                    this.libraryName.set(this@UniffiPlugin.libraryName)
//                    this.cargoOutputDirectory.set(cargoOutputDir)
//                    this.outputDirectory.set(outputDir)
//                    this.useCross.set(false)
//                }
//
//
//                val libFile = outputDir.map {
//                    it.file(
//                        BuildTarget.RustTarget.forCurrentPlatform.dynamicLibraryName(libraryName)!!
//                    )
//                }
//
//                project.tasks.named<BuildBindingsTask>(BUILD_BINDINGS_TASK_NAME) {
//                    packageDirectory.set(cargoExtension.packageDirectory)
//                    cargoMetadata.set(cargoMetadataProvider)
//                    bindgen.set(project.layout.buildDirectory.file("bindgen-install/bin/$bindgenName"))
//                    libraryFile.set(libFile)
//                    generateBindingsForExternalCrates.set(uniffiExtension.generateBindingsForExternalCrates)
//
//                    dependsOn(BUILD_LIB_FOR_BINDINGS_TASK_NAME)
//                    dependsOn(INSTALL_BINDGEN_TASK_NAME)
//                }
//            }
//            is BindingsGenerationFromUdl -> {
//                project.tasks.named<BuildBindingsTask>(BUILD_BINDINGS_TASK_NAME) {
//                    packageDirectory.set(cargoExtension.packageDirectory)
//                    cargoMetadata.set(cargoMetadataProvider)
//                    bindgen.set(project.layout.buildDirectory.file("bindgen-install/bin/$bindgenName"))
//                    udlFile.set(generation.udlFile)
//
//                    generateBindingsForExternalCrates.set(uniffiExtension.generateBindingsForExternalCrates)
//
//                    dependsOn(INSTALL_BINDGEN_TASK_NAME)
//                }
//            }
//        }
//    }
//
//    /**
//     * Configures the targets' sourceSets and dependencies
//     */
//    private fun configureTargets(
//        project: Project,
//        kmpExtension: KotlinMultiplatformExtension,
//    ) {
//        // Configure common main
//        val commonMain = kmpExtension.sourceSets.maybeCreate("commonMain")
//        commonMain
//            .kotlin
//            .srcDir(project.layout.buildDirectory.dir("generated/uniffi/commonMain"))
//        commonMain.dependencies {
//            if (uniffiExtension.addRuntime.get()) {
//                implementation("ch.ubique.uniffi:runtime:${Constants.RUNTIME_VERSION}")
//            }
//
//            if (uniffiExtension.addDependencies.get()) {
//                implementation("com.squareup.okio:okio:${Constants.OKIO_VERSION}")
//                implementation("org.jetbrains.kotlinx:atomicfu:${Constants.ATOMICFU_VERSION}")
//                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Constants.COROUTINES_VERSION}")
//                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Constants.DATETIME_VERSION}")
//            }
//        }
//
//        val targets = kmpExtension.targets
//            .mapNotNull { target ->
//                BuildTarget.fromTargetName(target.name)
//                    ?.let { Pair(it, target) }
//            }
//
//        val namespace = uniffiExtension.bindingsGeneration.get().namespace.get()
//
//        if (targets.any { (buildTarget, _) -> buildTarget in BuildTarget.nativeTargets }) {
//            configureBaseNativeTarget(
//                project,
//                kmpExtension.sourceSets.maybeCreate("nativeMain"),
//            )
//        }
//
//        // Configure all other build targets
//        targets.forEach { (buildTarget, kotlinTarget) ->
//            when (buildTarget) {
//                BuildTarget.Jvm -> configureJvmTarget(
//                    project,
//                    kmpExtension.sourceSets.getByName("jvmMain")
//                )
//
//                BuildTarget.Android -> configureAndroidTarget(
//                    project,
//                    kmpExtension.sourceSets.getByName("androidMain"),
//                    kmpExtension.sourceSets.maybeCreate("androidUnitTest")
//                )
//
//                in BuildTarget.nativeTargets -> configureNativeTarget(
//                    project,
//                    buildTarget,
//                    kotlinTarget as KotlinNativeTarget,
//                    namespace
//                )
//
//                else -> throw GradleException("Unhandled build target: ${buildTarget.name}")
//            }
//        }
//    }
//
//    private fun configureJvmTarget(
//        project: Project,
//        jvmMain: KotlinSourceSet
//    ) {
//        // Add generated bindings as a source directory
//        jvmMain
//            .kotlin
//            .srcDir(project.layout.buildDirectory.dir("generated/uniffi/jvmMain"))
//
//        // Add rust libraries to the resource path
//        jvmMain
//            .resources
//            .srcDir(
//                project
//                    .layout
//                    .buildDirectory
//                    .dir("intermediates/rust/jvmMain/resources/${releaseString(isRelease)}")
//            )
//
//        // Add JNA dependencies
//        jvmMain.dependencies {
//            implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
//        }
//
//        val copyNativeLibsTask = project.tasks.named(
//            copyNativeLibrariesTaskName(
//                BuildTarget.Jvm,
//                isRelease,
//                dynamic = true
//            )
//        )
//
//        // Hook into the JVM build process
//        project.tasks.named("jvmProcessResources") {
//            dependsOn(copyNativeLibsTask)
//        }
//    }
//
//    private fun configureAndroidTarget(
//        project: Project,
//        androidMain: KotlinSourceSet,
//        androidUnitTest: KotlinSourceSet,
//    ) {
//        // Add generated bindings as a source directory
//        androidMain
//            .kotlin
//            .srcDir(project.layout.buildDirectory.dir("generated/uniffi/androidMain"))
//
//        // Add aar version of JNA for android
//        androidMain.dependencies {
//            implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}@aar")
//        }
//
//        // Android unit tests run locally, so add the normal JNA dependency
//        androidUnitTest.dependencies {
//            implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
//        }
//
//        // For android builds, add rust libraries as to jni libs
//        val androidExtension = project.extensions.getByType<AndroidExtension>()
//        androidExtension
//            .sourceSets
//            .getByName("release")
//            .jniLibs
//            .setSrcDirs(listOf(project.layout.buildDirectory.dir("intermediates/rust/androidMain/jniLibs/Release")))
//        androidExtension
//            .sourceSets
//            .getByName("debug")
//            .jniLibs
//            .setSrcDirs(listOf(project.layout.buildDirectory.dir("intermediates/rust/androidMain/jniLibs/Debug")))
//        androidExtension
//            .sourceSets
//            .getByName("testRelease")
//            .jniLibs
//            .setSrcDirs(listOf(project.layout.buildDirectory.dir("intermediates/rust/androidMain/jniLibs/Release")))
//        androidExtension
//            .sourceSets
//            .getByName("testDebug")
//            .jniLibs
//            .setSrcDirs(listOf(project.layout.buildDirectory.dir("intermediates/rust/androidMain/jniLibs/Debug")))
//
//        // The "debug" source set is used for local tests, add it the native rust library
//        // as a resource here (same as jvm).
//        androidExtension
//            .sourceSets
//            .getByName("testDebug")
//            .resources
//            .srcDir(project.layout.buildDirectory.dir("intermediates/rust/androidMain/resources/Debug"))
//        androidExtension
//            .sourceSets
//            .getByName("testRelease")
//            .resources
//            .srcDir(project.layout.buildDirectory.dir("intermediates/rust/androidMain/resources/Release"))
//
//        // Hook into the Android build process
//        project.tasks.withType<MergeSourceSetFolders> {
//            val release = if (name.contains("release", ignoreCase = true)) {
//                true
//            } else if (name.contains("debug", ignoreCase = true)) {
//                false
//            } else {
//                isRelease
//            }
//
//            val copyNativeLibsTask =
//                copyNativeLibrariesTaskName(BuildTarget.Android, release, dynamic = true)
//            inputs.dir(
//                project.layout.buildDirectory.dir(
//                    "intermediates/rust/androidMain/jniLibs/${
//                        releaseString(
//                            release
//                        )
//                    }"
//                )
//            )
//            dependsOn(copyNativeLibsTask)
//        }
//
//        project.tasks.named("packageDebugResources") {
//            dependsOn(
//                copyNativeLibrariesTaskName(
//                    BuildTarget.Android,
//                    release = false,
//                    dynamic = true
//                )
//            )
//        }
//
//        project.tasks.named("packageReleaseResources") {
//            dependsOn(
//                copyNativeLibrariesTaskName(
//                    BuildTarget.Android,
//                    release = true,
//                    dynamic = true
//                )
//            )
//        }
//    }
//
//    private fun configureBaseNativeTarget(
//        project: Project,
//        nativeMain: KotlinSourceSet,
//    ) {
//        nativeMain
//            .kotlin
//            .srcDir(project.layout.buildDirectory.dir("generated/uniffi/nativeMain"))
//
//        // Dummy def file is the same for all native targets
//        configureGenerateDummyDefFileTask(project)
//    }
//
//    private fun configureNativeTarget(
//        project: Project,
//        buildTarget: BuildTarget,
//        nativeTarget: KotlinNativeTarget,
//        namespace: String,
//    ) {
//        val generateDefFileTask = configureGenerateDefFileTask(project, buildTarget)
//        val generatedDefFile = generateDefFileTask.map { it.outputFile }.get()
//
//        val generateDummyDefFileTask =
//            project.tasks.named<GenerateDummyDefFileTask>(GENERATE_DUMMY_DEF_FILE)
//        val dummyDefFile = generateDummyDefFileTask.map { it.outputFile }.get()
//
//        // As native targets need to be configured separately per architecture
//        // there should be exactly one rust target for the build target.
//        val rustTarget = buildTarget.checkedNativeTarget
//
//        val copyNativeLibsTask = project.tasks.named(
//            copyNativeLibrariesTaskName(
//                rustTarget,
//                buildTarget,
//                isRelease,
//                buildTarget.useDynamicLib == true
//            ),
//            CopyNativeLibrariesTask::class.java
//        )
//        val libraryIncludeDir = copyNativeLibsTask.get().outputDir.asFile.get().absolutePath
//
//        val isSync = project.gradle.startParameter.taskNames.isEmpty()
//                && (project.findProperty("forceBuildNativeLibs") != "true")
//
//        nativeTarget.compilations.getByName("main") {
//            cinterops.register("uniffi") {
//                packageName("cinterop")
//
//                if (isSync) {
//                    defFile(dummyDefFile)
//                } else {
//                    defFile(generatedDefFile)
//
//                    extraOpts(
//                        "-libraryPath",
//                        libraryIncludeDir
//                    )
//                }
//
//                project.tasks.named(interopProcessingTaskName) {
//                    // Generates the headers file
//                    dependsOn(BUILD_BINDINGS_TASK_NAME)
//
//                    if (isSync) {
//                        dependsOn(generateDummyDefFileTask)
//                    } else {
//                        // Generates the .def file
//                        dependsOn(generateDefFileTask)
//                        // Copy native libraries of this is not a sync
//                        dependsOn(copyNativeLibsTask)
//                    }
//                }
//            }
//        }
//
//        nativeTarget.compilerOptions {
//            optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
//        }
//    }
//
//    /**
//     * Registers all possible build tasks without actually running them
//     */
//    private fun registerBuildTasks(project: Project) {
//        // Register the build tasks for each rust target.
//        // This task will run `cargo build --target <target>`
//        // for the corresponding target.
//        for (target in BuildTarget.RustTarget.entries) {
//            for (release in listOf<Boolean>(true, false)) {
//                val taskName = cargoBuildTaskName(target, release)
//                project.tasks.register<CargoBuildTask>(taskName)
//            }
//        }
//
//        // Once the libraries are built, they need to be referenced as a
//        // resource in the JAR / APK, this task explicitly copies the libraries
//        // into a separate folder, that is registered as a resource folder.
//        for (buildTarget in BuildTarget.entries) {
//            for (dynamic in listOf<Boolean>(true, false)) {
//                // For debug targets
//                buildTarget.debugTargetsAll.forEach { rustTarget ->
//                    val copyTaskName =
//                        copyNativeLibrariesTaskName(rustTarget, buildTarget, false, dynamic)
//                    project.tasks.register<CopyNativeLibrariesTask>(copyTaskName)
//                }
//                // For release targets
//                buildTarget.releaseTargetsAll.forEach { rustTarget ->
//                    val copyTaskName =
//                        copyNativeLibrariesTaskName(rustTarget, buildTarget, true, dynamic)
//                    project.tasks.register<CopyNativeLibrariesTask>(copyTaskName)
//                }
//
//                // Create a unified task that copies all the libraries for the buildTarget (debug)
//                project.tasks.register(copyNativeLibrariesTaskName(buildTarget, false, dynamic))
//                // Create a unified task that copies all the libraries for the buildTarget (release)
//                project.tasks.register(copyNativeLibrariesTaskName(buildTarget, true, dynamic))
//            }
//        }
//    }
//
//    private fun configureBuildTasks(
//        project: Project,
//        rustSourceDir: DirectoryProperty,
//    ) {
//        val rustTargetDir =
//            project.objects.directoryProperty().fileValue(File(cargoMetadata.targetDirectory))
//        val buildOutputDir = project.layout.buildDirectory.dir("target")
//
//        val androidExtension = project.extensions.findByType<AndroidExtension>()
//
//        // Register the build tasks for each rust target.
//        // This task will run `cargo build --target <target>`
//        // for the corresponding target.
//        for (target in BuildTarget.RustTarget.entries) {
//            val config = cargoExtension.compilations.getByName(target.name)
//
//            for (release in listOf<Boolean>(true, false)) {
//                val taskName = cargoBuildTaskName(target, release)
//                val cargoOutputDir = if (release) {
//                    rustTargetDir.dir("${target.rustTriple}/release")
//                } else {
//                    rustTargetDir.dir("${target.rustTriple}/debug")
//                }
//                val outputDir = if (release) {
//                    buildOutputDir.map { it.dir("${target.rustTriple}/release") }
//                } else {
//                    buildOutputDir.map { it.dir("${target.rustTriple}/debug") }
//                }
//
//                project.tasks.named<CargoBuildTask>(taskName) {
//                    this.packageDirectory.set(rustSourceDir)
//                    this.triple.set(target.rustTriple)
//                    this.release.set(release)
//                    this.packageName.set(targetPackage.name)
//                    this.libraryName.set(this@UniffiPlugin.libraryName)
//                    this.cargoOutputDirectory.set(cargoOutputDir)
//                    this.outputDirectory.set(outputDir)
//                    this.useCross.set(config.useCross)
//
//                    if (target.isAndroid && androidExtension != null) {
//                        val sdkRoot = androidExtension.sdkDirectory
//                        val apiLevel = androidExtension.defaultConfig.minSdk ?: 21
//                        val ndkVersion = androidExtension.ndkVersion.takeIf(String::isNotEmpty)
//                        val ndkRoot = androidExtension.ndkPath?.let { File(it) }
//
//                        val ndkEnv = NdkUtil.ndkEnvVariables(
//                            sdkRoot,
//                            apiLevel,
//                            ndkVersion,
//                            ndkRoot,
//                            target.rustTriple,
//                            target.ndkLlvmTriple
//                        )
//
//                        if (!config.useCross.get()) {
//                            // When using cross, it will manage the correct environmental variables
//                            this.additionalEnvironment.set(ndkEnv)
//                        }
//                    }
//                }
//            }
//        }
//
//        // Once the libraries are built, they need to be referenced as a
//        // resource in the JAR / APK, this task explicitly copies the libraries
//        // into a separate folder, that is registered as a resource folder.
//        for (buildTarget in BuildTarget.entries) {
//            for (dynamic in listOf<Boolean>(true, false)) {
//                // For debug targets
//                buildTarget.debugTargetsAll.forEach { rustTarget ->
//                    configureCopyNativeLibrariesTask(
//                        project,
//                        buildOutputDir,
//                        buildTarget,
//                        rustTarget,
//                        false,
//                        dynamic,
//                        targetPackage.targets.first().name,
//                    )
//                }
//                // For release targets
//                buildTarget.releaseTargetsAll.forEach { rustTarget ->
//                    configureCopyNativeLibrariesTask(
//                        project,
//                        buildOutputDir,
//                        buildTarget,
//                        rustTarget,
//                        true,
//                        dynamic,
//                        targetPackage.targets.first().name
//                    )
//                }
//            }
//        }
//
//        val abiFilters = androidExtension?.defaultConfig?.ndk?.abiFilters?.toList() ?: listOf()
//
//        for (buildTarget in BuildTarget.entries) {
//            for (dynamic in listOf<Boolean>(true, false)) {
//                val debugTargets = if (buildTarget == BuildTarget.Android && abiFilters.isNotEmpty()) {
//                    buildTarget.debugTargets.filter { abiFilters.contains(it.abiName) }
//                } else {
//                    buildTarget.debugTargets
//                } + buildTarget.baseTargets
//
//                val releaseTargets = if (buildTarget == BuildTarget.Android && abiFilters.isNotEmpty()) {
//                    buildTarget.releaseTargets.filter { abiFilters.contains(it.abiName) }
//                } else {
//                    buildTarget.releaseTargets
//                } + buildTarget.baseTargets
//
//                // Create a unified task that copies all the libraries for the buildTarget (debug)
//                project.tasks.named(copyNativeLibrariesTaskName(buildTarget, false, dynamic)) {
//                    dependsOn(debugTargets.map {
//                        copyNativeLibrariesTaskName(it, buildTarget, false, dynamic)
//                    })
//                }
//                // Create a unified task that copies all the libraries for the buildTarget (release)
//                project.tasks.named(copyNativeLibrariesTaskName(buildTarget, true, dynamic)) {
//                    dependsOn(releaseTargets.map {
//                        copyNativeLibrariesTaskName(it, buildTarget, true, dynamic)
//                    })
//                }
//            }
//        }
//    }
//
//    private fun configureCopyNativeLibrariesTask(
//        project: Project,
//        buildDir: Provider<Directory>,
//        buildTarget: BuildTarget,
//        rustTarget: BuildTarget.RustTarget,
//        release: Boolean,
//        dynamic: Boolean,
//        packageName: String,
//    ) {
//        val cargoBuildTask = cargoBuildTaskName(rustTarget, release)
//        val copyTaskName = copyNativeLibrariesTaskName(rustTarget, buildTarget, release, dynamic)
//
//        val buildDir = if (release) {
//            buildDir.map { it.dir("${rustTarget.rustTriple}/release") }
//        } else {
//            buildDir.map { it.dir("${rustTarget.rustTriple}/debug") }
//        }
//        val outputDir = if (rustTarget.abiName != null) {
//            project
//                .layout
//                .buildDirectory
//                .dir(
//                    "intermediates/rust/${buildTarget.sourceSetName}/jniLibs/${
//                        releaseString(
//                            release
//                        )
//                    }/${rustTarget.abiName}"
//                )
//        } else {
//            project
//                .layout
//                .buildDirectory
//                .dir(
//                    "intermediates/rust/${buildTarget.sourceSetName}/resources/${
//                        releaseString(
//                            release
//                        )
//                    }/${rustTarget.jarLibraryPath}"
//                )
//        }
//
//        val libraryFileName = if (dynamic) {
//            rustTarget.dynamicLibraryName(packageName)
//        } else {
//            rustTarget.staticLibraryName(packageName)
//        } ?: throw GradleException("Could not determine library file name!")
//        val libraryFile = buildDir.map { it.file(libraryFileName) }
//
//        project.tasks.named<CopyNativeLibrariesTask>(copyTaskName) {
//            this.libraryFile.set(libraryFile)
//            this.outputDir.set(outputDir)
//
//            dependsOn(cargoBuildTask)
//        }
//    }
//
//    /**
//     * Registers the GenerateDefFileTask, needed for native targets.
//     */
//    private fun registerGenerateDefFileTask(project: Project) {
//        for (buildTarget in BuildTarget.entries) {
//            project.tasks.register<GenerateDefFileTask>(generateDefFileTaskName(buildTarget))
//        }
//        project.tasks.register<GenerateDummyDefFileTask>(GENERATE_DUMMY_DEF_FILE)
//    }
//
//    /**
//     * Configures the GenerateDefFileTask, needed for native targets.
//     */
//    private fun configureGenerateDefFileTask(
//        project: Project,
//        buildTarget: BuildTarget,
//    ): TaskProvider<GenerateDefFileTask> {
//        val defFile = project
//            .layout
//            .buildDirectory
//            .file("generated/uniffi/nativeInterop/cinterop/$libraryName-${buildTarget.name}.def")
//
//        val rustTarget = buildTarget.checkedNativeTarget
//        val staticLibName = rustTarget.staticLibraryName(libraryName)
//
//        val headersDir = project
//            .layout
//            .buildDirectory
//            .dir("generated/uniffi/nativeInterop/cinterop/headers/")
//
//        val config = cargoExtension.compilations.getByName(rustTarget.name)
//
//        val generateDefFileTask =
//            project.tasks.named<GenerateDefFileTask>(generateDefFileTaskName(buildTarget)) {
//                this.libraryName.set(staticLibName)
//                this.outputFile.set(defFile)
//                this.packageDirectory.set(cargoExtension.packageDirectory)
//                this.targetString.set(rustTarget.rustTriple)
//                this.headersDir.set(headersDir)
//                this.useCross.set(config.useCross)
//
//                dependsOn(BUILD_BINDINGS_TASK_NAME)
//            }
//
//        return generateDefFileTask
//    }
//
//    /**
//     * Configures the GenerateDummyDefFileTask, needed to make syncs go fast.
//     */
//    private fun configureGenerateDummyDefFileTask(project: Project) {
//        val dummyDefFile =
//            project.layout.buildDirectory.file("generated/uniffi/nativeInterop/cinterop/dummy.def")
//
//        val headersDir = project
//            .layout
//            .buildDirectory
//            .dir("generated/uniffi/nativeInterop/cinterop/headers/")
//
//        project.tasks.named<GenerateDummyDefFileTask>(GENERATE_DUMMY_DEF_FILE) {
//            this.outputFile.set(dummyDefFile)
//            this.headersDir.set(headersDir)
//
//            dependsOn(BUILD_BINDINGS_TASK_NAME)
//        }
//    }
//
//    private fun currentBuildVariant(project: Project): CargoBuildVariant? {
//        val releaseBuildProperty = project.findProperty("releaseBuild")
//        if (releaseBuildProperty == "true") return CargoBuildVariant.Release
//
//        val taskNames = project.gradle.startParameter.taskNames
//
//        // This environmental variable might be set by XCode
//        val configuration = System.getenv("CONFIGURATION") ?: ""
//        // NOTE: There is no better way of knowing what variant is currently built
//        val isDebug = taskNames.any { it.contains("Debug", ignoreCase = true) }
//                || configuration.contains("Debug", ignoreCase = true)
//        val isRelease = taskNames.any { it.contains("Release", ignoreCase = true) }
//                || configuration.contains("Release", ignoreCase = true)
//
//        if (isRelease) return CargoBuildVariant.Release
//        if (isDebug) return CargoBuildVariant.Debug
//        return null
//    }
}
