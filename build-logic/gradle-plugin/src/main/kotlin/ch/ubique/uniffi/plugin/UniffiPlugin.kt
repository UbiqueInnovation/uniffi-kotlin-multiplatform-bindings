package ch.ubique.uniffi.plugin

import ch.ubique.uniffi.plugin.dsl.BindingsGenerationFromLibrary
import ch.ubique.uniffi.plugin.dsl.BindingsGenerationFromUdl
import ch.ubique.uniffi.plugin.dsl.CargoExtension
import ch.ubique.uniffi.plugin.dsl.UniffiExtension
import ch.ubique.uniffi.plugin.android.AndroidSupport
import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.model.CargoInfo
import ch.ubique.uniffi.plugin.model.CargoMetadata
import ch.ubique.uniffi.plugin.model.CrateType
import ch.ubique.uniffi.plugin.services.CargoMetadataService
import ch.ubique.uniffi.plugin.tasks.BuildBindingsTask
import ch.ubique.uniffi.plugin.tasks.CargoBuildTask
import ch.ubique.uniffi.plugin.tasks.GenerateDefFileTask
import ch.ubique.uniffi.plugin.tasks.GenerateDummyDefFileTask
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
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
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

        /** The merged library directory of a source set, relative to the project */
        private fun librariesPath(sourceSetName: String): String =
            "$PREFIX/build/intermediates/$sourceSetName/libs"

        /** C-Interop name for native targets */
        private const val CINTEROP_NAME: String = "$PREFIX-cinterop"

        /** C-Interop name for native targets */
        private const val CINTEROP_PACKAGE_NAME: String = "cinterop"

        /** Where the generated def files are written, relative to the project */
        private const val CINTEROP_DEF_PATH: String = "$PREFIX/cinterop"
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
            targetDirectory = cargoExtension.targetDirectory.orElse(
                project.layout.dir(metadata.map { File(it.targetDirectory) })
            ),
        )

        // Set up the bindings tasks
        val installBindgenTask = project.registerInstallBindgenTask()
        val buildLibraryForBindingsTask = project.registerBuildLibraryForBindingsTask(cargoInfo)
        val buildBindingsTask = project.registerBuildBindingsTask(
            bindgenBin = installBindgenTask.flatMap { it.bindgenBinPath },
            libraryForBindings = buildLibraryForBindingsTask.flatMap { it.dynamicLibraryFile },
            metadataJson = metadataJsonProvider,
        )

        var hasNativeTarget: Boolean = false
        project.pluginManager.withPlugin(Plugins.KMP_PLUGIN) {
            val kmpExtension =
                project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            // The generated bindings declare `expect`/`actual` classes for every type that
            // crosses the FFI, which is still flagged as Beta (KT-61573). Without this every
            // consumer gets a warning per declaration for code they did not write.
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            kmpExtension.compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }

            val commonMain = kmpExtension.sourceSets.getByName("commonMain")
            project.configureCommonMain(commonMain, buildBindingsTask.flatMap { it.commonMainDir })

            kmpExtension.targets.configureEach { target ->
                val buildTarget = BuildTarget.fromTargetName(target.name) ?: return@configureEach

                when (buildTarget) {
                    BuildTarget.Jvm ->
                        project.configureJvmTarget(
                            jvmMain = kmpExtension.sourceSets.maybeCreate("jvmMain"),
                            bindingsDir = buildBindingsTask.flatMap { it.jvmMainDir },
                            librariesDir = project.registerMergeLibrariesTask(
                                taskName = Tasks.MERGE_JVM_RESOURCES,
                                buildTarget = BuildTarget.Jvm,
                                cargoInfo = cargoInfo,
                                isRelease = isRelease,
                            ).flatMap { it.outputDirectory },
                        )

                    BuildTarget.Android ->
                        project.configureAndroidTarget(
                            kmpExtension = kmpExtension,
                            androidMain = kmpExtension.sourceSets.maybeCreate("androidMain"),
                            bindingsDir = buildBindingsTask.flatMap { it.androidMainDir },
                            jniLibrariesTask = project.registerMergeLibrariesTask(
                                taskName = Tasks.MERGE_ANDROID_JNI_LIBS,
                                buildTarget = BuildTarget.Android,
                                cargoInfo = cargoInfo,
                                isRelease = isRelease,
                                rustTargets = BuildTarget.Android.rustTargets(isRelease)
                                    .filter { it.abiName != null },
                                leafName = { it.abiName!! }
                            ),
                            hostLibrariesTask = project.registerMergeLibrariesTask(
                                taskName = Tasks.MERGE_ANDROID_TEST_RESOURCES,
                                buildTarget = BuildTarget.AndroidLocal,
                                cargoInfo = cargoInfo,
                                isRelease = isRelease,
                                leafName = { it.jarLibraryPath }
                            )
                        )

                    in BuildTarget.nativeTargets -> {
                        hasNativeTarget = true

                        project.configureNativeTarget(
                            nativeMain = kmpExtension.sourceSets.maybeCreate("nativeMain"),
                            nativeTarget = target as KotlinNativeTarget,
                            bindingsDir = buildBindingsTask.flatMap { it.nativeMainDir },
                            defFile = project.registerDefFileTask(
                                buildTarget = buildTarget,
                                cargoInfo = cargoInfo,
                                isRelease = isRelease,
                                isSync = isSync,
                                headersDir = buildBindingsTask.flatMap { it.nativeInteropHeadersDir },
                            ),
                            staticLibrary = if (isSync) {
                                null
                            } else {
                                project.registerCargoBuildTask(
                                    rustTarget = buildTarget.checkedNativeTarget,
                                    release = isRelease,
                                    cargoInfo = cargoInfo,
                                    crateType = CrateType.SystemStaticLibrary,
                                ).flatMap { it.staticLibraryFile }
                            },
                        )
                    }

                    else -> throw GradleException("Unhandled target: $buildTarget")
                }
            }
        }

        project.afterEvaluate { evaluated ->
            // Make sure the KMP Plugin is applied
            if (!evaluated.plugins.hasPlugin(Plugins.KMP_PLUGIN)) {
                throw GradleException("Kotlin Multiplatform Plugin is required")
            }

            // Make sure the binding generation source is specified
            if (!uniffiExtension.bindingsGeneration.isPresent) {
                throw GradleException("Please call either 'generateFromLibrary' or 'generateFromUdl'.")
            }

            // The generated bindings live in `nativeMain`, a source set shared by all
            // Kotlin/Native targets, and reference the c-interop declarations. Only the
            // commonizer makes those visible from a shared source set, so without it the
            // build fails deep inside the Kotlin compiler with unresolved references.
            if (hasNativeTarget && "commonizeCInterop" !in evaluated.tasks.names) {
                throw GradleException(
                    "Please set 'kotlin.mpp.enableCInteropCommonization=true' in gradle.properties"
                )
            }

            if ("prepareKotlinIdeaImport" in evaluated.tasks.names) {
                evaluated.tasks.named("prepareKotlinIdeaImport") { task ->
                    task.dependsOn(buildBindingsTask)
                }
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
            task.crateTypes.add(CrateType.SystemDynamicLibrary)
            task.rustcWrapper.set(cargoExtension.rustcWrapper)
            task.rustcWorkspaceWrapper.set(cargoExtension.rustcWorkspaceWrapper)
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
            task.formatCode.set(uniffiExtension.formatCode)

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
        crateType: CrateType,
    ): TaskProvider<CargoBuildTask> = tasks.maybeRegister(
        Tasks.cargoBuild(rustTarget, release),
        CargoBuildTask::class.java,
    ) { task ->
        task.packageDirectory.set(cargoExtension.packageDirectory)
        task.rustTarget.set(rustTarget)
        task.release.set(release)
        task.packageName.set(cargoInfo.packageName)
        task.libraryName.set(cargoInfo.libraryName)
        task.cargoTargetDirectory.set(cargoInfo.targetDirectory)
        task.crateTypes.add(crateType)
        task.rustcWrapper.set(cargoExtension.rustcWrapper)
        task.rustcWorkspaceWrapper.set(cargoExtension.rustcWorkspaceWrapper)

        val useCross = cargoExtension.compilations.getByName(rustTarget.name).useCross
        task.useCross.set(useCross)

        // An android rust target can only be requested once the android plugin created the
        // android build target, so touching AndroidSupport - and with it AGP - is safe here.
        if (rustTarget.isAndroid) {
            task.additionalEnvironment.set(
                AndroidSupport(project).ndkEnvironment(
                    rustTarget = rustTarget,
                    useCross = useCross,
                    ndkVersion = cargoExtension.ndkVersion,
                )
            )
        }
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
        leafName: (BuildTarget.RustTarget) -> String = { it.jarLibraryPath },
    ): TaskProvider<MergeLibrariesTask> {
        val cargoBuilds = rustTargets.map { rustTarget ->
            rustTarget to registerCargoBuildTask(
                rustTarget = rustTarget,
                release = isRelease,
                cargoInfo = cargoInfo,
                crateType = if (buildTarget.usesDynamicLibrary) {
                    CrateType.SystemDynamicLibrary
                } else {
                    CrateType.SystemStaticLibrary
                },
            )
        }
        return tasks.register(taskName, MergeLibrariesTask::class.java) { task ->
            task.outputDirectory.convention(
                layout.buildDirectory.dir(librariesPath(buildTarget.sourceSetName))
            )

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

    /**
     * Registers a [GenerateDefFileTask] (or [GenerateDummyDefFileTask] during sync) and returns the
     * output file.
     */
    private fun Project.registerDefFileTask(
        buildTarget: BuildTarget,
        cargoInfo: CargoInfo,
        isRelease: Boolean,
        isSync: Boolean,
        headersDir: Provider<Directory>,
    ): Provider<RegularFile> = if (isSync) {
        registerGenerateDummyDefFileTask(headersDir).flatMap { it.outputFile }
    } else {
        registerGenerateDefFileTask(buildTarget, cargoInfo, isRelease, headersDir)
            .flatMap { it.outputFile }
    }

    private fun Project.registerGenerateDefFileTask(
        buildTarget: BuildTarget,
        cargoInfo: CargoInfo,
        isRelease: Boolean,
        headersDir: Provider<Directory>,
    ): TaskProvider<GenerateDefFileTask> {
        val rustTarget = buildTarget.checkedNativeTarget
        val config = cargoExtension.compilations.getByName(rustTarget.name)
        val cargoBuild = registerCargoBuildTask(
            rustTarget = rustTarget,
            release = isRelease,
            cargoInfo = cargoInfo,
            crateType = CrateType.SystemStaticLibrary,
        )

        return tasks.maybeRegister(
            Tasks.generateDefFile(buildTarget),
            GenerateDefFileTask::class.java,
        ) { task ->
            task.staticLibrary.set(cargoBuild.flatMap { it.staticLibraryFile })
            task.outputFile.set(
                project.layout.buildDirectory.file("$CINTEROP_DEF_PATH/uniffi-${buildTarget.name}.def")
            )
            task.packageDirectory.set(cargoExtension.packageDirectory)
            task.cargoTargetDirectory.set(cargoInfo.targetDirectory)
            task.targetString.set(rustTarget.rustTriple)
            // Carries the dependency on the bindings.
            task.headersDir.set(headersDir)
            task.useCross.set(config.useCross)
            task.rustcWrapper.set(cargoExtension.rustcWrapper)
            task.rustcWorkspaceWrapper.set(cargoExtension.rustcWorkspaceWrapper)
        }
    }

    private fun Project.registerGenerateDummyDefFileTask(
        headersDir: Provider<Directory>,
    ): TaskProvider<GenerateDummyDefFileTask> =
        tasks.maybeRegister(
            Tasks.GENERATE_DUMMY_DEF,
            GenerateDummyDefFileTask::class.java,
        ) { task ->
            task.outputFile.set(project.layout.buildDirectory.file("$CINTEROP_DEF_PATH/dummy.def"))
            task.headersDir.set(headersDir)
        }

    private fun Project.configureCommonMain(
        commonMain: KotlinSourceSet,
        bindingsDir: Provider<Directory>,
    ) {
        commonMain.kotlin.srcDir(bindingsDir)

        configurations.named(commonMain.implementationConfigurationName) { configuration ->
            configuration.dependencies.addIf(
                condition = uniffiExtension.addRuntime,
                dependencies.create("ch.ubique.uniffi:runtime:${Constants.RUNTIME_VERSION}")
            )

            configuration.dependencies.addIf(
                condition = uniffiExtension.addDependencies,
                dependencies.create("com.squareup.okio:okio:${Constants.OKIO_VERSION}"),
                dependencies.create("org.jetbrains.kotlinx:atomicfu:${Constants.ATOMICFU_VERSION}"),
                dependencies.create("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Constants.COROUTINES_VERSION}"),
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

        configurations.named(jvmMain.implementationConfigurationName) { configuration ->
            configuration.dependencies.addIf(
                condition = uniffiExtension.addDependencies,
                dependencies.create("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
            )
        }
    }

    private fun Project.configureAndroidTarget(
        kmpExtension: KotlinMultiplatformExtension,
        androidMain: KotlinSourceSet,
        bindingsDir: Provider<Directory>,
        jniLibrariesTask: TaskProvider<MergeLibrariesTask>,
        hostLibrariesTask: TaskProvider<MergeLibrariesTask>,
    ) {
        androidMain.kotlin.srcDir(bindingsDir)

        // Add the JNA aar dependency
        configurations.named(androidMain.implementationConfigurationName) { configuration ->
            configuration.dependencies.addIf(
                condition = uniffiExtension.addDependencies,
                dependencies.create("net.java.dev.jna:jna:${Constants.JNA_VERSION}@aar")
            )
        }

        // Add the JNA jar dependency to the androidHostTest
        kmpExtension.sourceSets.configureEach { sourceSet ->
            if (sourceSet.name == "androidHostTest") {
                configurations.named(sourceSet.implementationConfigurationName) { configuration ->
                    configuration.dependencies.addIf(
                        condition = uniffiExtension.addDependencies,
                        dependencies.create("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
                    )
                }
            }
        }

        pluginManager.withPlugin(Plugins.ANDROID_PLUGIN) {
            AndroidSupport(project).wireVariants(jniLibrariesTask, hostLibrariesTask)
        }
    }

    private fun Project.configureNativeTarget(
        nativeMain: KotlinSourceSet,
        nativeTarget: KotlinNativeTarget,
        bindingsDir: Provider<Directory>,
        defFile: Provider<RegularFile>,
        staticLibrary: Provider<RegularFile>?,
    ) {
        nativeMain.kotlin.srcDir(bindingsDir)

        nativeTarget.compilations.getByName("main") { compilation ->
            compilation.cinterops.register(CINTEROP_NAME) { cinterop ->
                cinterop.packageName(CINTEROP_PACKAGE_NAME)

                cinterop.definitionFile.set(defFile)

                if (staticLibrary != null) {
                    // Add the static library as an input to the cinterop task, otherwise cinterop
                    // won't re-run on a new build of the library.
                    tasks.named(cinterop.interopProcessingTaskName) { task ->
                        task.inputs.file(staticLibrary)
                            .withPropertyName("uniffiStaticLibrary")
                            .withPathSensitivity(PathSensitivity.ABSOLUTE)
                    }
                }
            }
        }

        nativeTarget.compilerOptions { options ->
            options.optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    /**
     * Registers [name] if it is not registered yet, otherwise returns the existing provider and
     * applies [configure] so repeated requests can add requirements to the same Cargo task.
     */
    private fun <T : Task> TaskContainer.maybeRegister(
        name: String,
        type: Class<T>,
        configure: Action<T>,
    ): TaskProvider<T> = if (name in names) {
        named(name, type).also { it.configure(configure) }
    } else {
        register(name, type, configure)
    }

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
