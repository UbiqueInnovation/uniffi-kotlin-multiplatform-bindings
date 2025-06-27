package ch.ubique.uniffi.plugin

import com.android.build.gradle.BaseExtension as AndroidExtension
import ch.ubique.uniffi.plugin.dsl.CargoExtension
import ch.ubique.uniffi.plugin.dsl.UniffiExtension
import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.model.CargoBuildVariant
import ch.ubique.uniffi.plugin.model.CargoMetadata
import ch.ubique.uniffi.plugin.services.CargoMetadataService
import ch.ubique.uniffi.plugin.tasks.BuildBindingsTask
import ch.ubique.uniffi.plugin.tasks.CargoBuildTask
import ch.ubique.uniffi.plugin.tasks.GenerateDefFileTask
import ch.ubique.uniffi.plugin.tasks.GenerateDummyDefFileTask
import ch.ubique.uniffi.plugin.tasks.InstallBindgenTask
import ch.ubique.uniffi.plugin.utils.NdkUtil
import ch.ubique.uniffi.plugin.utils.targetPackage
import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import java.io.File

class UniffiPlugin : Plugin<Project> {
    companion object {
        private const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"

        private const val INSTALL_BINDGEN_TASK_NAME = "installBindgen"
        private const val BUILD_BINDINGS_TASK_NAME = "buildBindings"
        private const val GENERATE_DUMMY_DEF_FILE = "generateDummyDefFile"
    }

    private lateinit var cargoMetadata: CargoMetadata
    private val targetPackage: CargoMetadata.Package
        get() = cargoMetadata.targetPackage

    private val libraryName: String
        get() = targetPackage.targets.first().name

    private lateinit var buildVariant: CargoBuildVariant

    private val isRelease: Boolean
        get() = buildVariant == CargoBuildVariant.Release

    private lateinit var uniffiExtension: UniffiExtension

    private lateinit var cargoExtension: CargoExtension

    /**
     * Applies the Plugin to the target Project. Registers all tasks and
     * configures them, once the project has been evaluated.
     */
    override fun apply(project: Project) {
        // Create the extensions
        uniffiExtension = project.extensions.create<UniffiExtension>("uniffi")
        cargoExtension = project.extensions.create<CargoExtension>("cargo")

        // Register tasks
        registerBindgenTasks(project)

        registerBuildTasks(project)

        registerGenerateDefFileTask(project)

        // Configure tasks after evaluation
        project.afterEvaluate { afterEvaluate(this) }
    }

    /**
     * Runs after the project was evaluated. Checks whether all configuration is
     * as expected and configures the tasks accordingly.
     */
    private fun afterEvaluate(project: Project) {
        // Check if the KMP Plugin is applied
        if (!project.plugins.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID)) {
            throw GradleException("Kotlin Multiplatform Plugin is required!")
        }

        // Make sure the CInteropProcess Task will run
        if (project.tasks.find { it.name == "commonize" } == null) {
            throw GradleException("Please set 'kotlin.mpp.enableCInteropCommonization=true' in gradle.properties")
        }

        // Make sure the bindings generation is defined
        if (!uniffiExtension.bindingsGeneration.isPresent) {
            throw GradleException("Please call either 'generateFromLibrary' or 'generateFromUdl'.")
        }

        val cargoMetadataService = project.providers.of(CargoMetadataService::class.java) {
            parameters.packageDirectory.set(cargoExtension.packageDirectory)
        }

        cargoMetadata = CargoMetadata.fromJsonString(cargoMetadataService.get())

        uniffiExtension.bindingsGeneration.get().namespace.convention(libraryName)

        configureBindgenTasks(project, cargoMetadataService)

        configureBuildTasks(
            project,
            cargoExtension.packageDirectory
        )

        // The CInteropProcess Task will run on Sync if
        // 'kotlin.mpp.enableCInteropCommonization' is
        // set to true.
        // Use it to hook into the Sync process and
        // build the bindings.
        project.tasks.named("commonize") {
            dependsOn(BUILD_BINDINGS_TASK_NAME)
        }

        // Build the debug variant if unsure, the release
        // variant has to be explicitly requested.
        buildVariant = currentBuildVariant(project)
            ?: CargoBuildVariant.Debug

        // Configure the different targets
        project.plugins.withId(KOTLIN_MULTIPLATFORM_PLUGIN_ID) {
            val kotlinExt = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

            configureTargets(project, kotlinExt)
        }

        // Make sure the bindings are built before kotlin code is compiled
        project.tasks.withType<KotlinCompilationTask<*>> {
            dependsOn(BUILD_BINDINGS_TASK_NAME)
        }
        project.tasks.withType<Jar> {
            dependsOn(BUILD_BINDINGS_TASK_NAME)
        }
        project.tasks.withType<CInteropProcess> {
            dependsOn(BUILD_BINDINGS_TASK_NAME)
        }
    }

    /**
     * Registers the InstallBindgen and BuildBindings Task
     */
    private fun registerBindgenTasks(project: Project) {
        project.tasks.register<InstallBindgenTask>(INSTALL_BINDGEN_TASK_NAME)

        project.tasks.register<BuildBindingsTask>(BUILD_BINDINGS_TASK_NAME)
    }

    /**
     * Configures the InstallBindgen and BuildBindings Task
     */
    private fun configureBindgenTasks(
        project: Project,
        cargoMetadataProvider: Provider<String>
    ) {
        project.tasks.named<InstallBindgenTask>(INSTALL_BINDGEN_TASK_NAME) {
            bindgenPath.set(project.layout.buildDirectory.dir("bindgen-install"))
            bindgenTmpPath.set(project.rootProject.layout.buildDirectory.dir("bindgen-install/target"))
            source.set(uniffiExtension.bindgenSource)
        }

        val bindgenName =
            uniffiExtension.bindgenSource.get().bindgenName ?: Constants.BINDGEN_BIN_NAME

        project.tasks.named<BuildBindingsTask>(BUILD_BINDINGS_TASK_NAME) {
            packageDirectory.set(cargoExtension.packageDirectory)
            cargoMetadata.set(cargoMetadataProvider)
            bindgen.set(project.layout.buildDirectory.file("bindgen-install/bin/$bindgenName"))

            dependsOn(INSTALL_BINDGEN_TASK_NAME)
        }
    }

    /**
     * Configures the targets' sourceSets and dependencies
     */
    private fun configureTargets(
        project: Project,
        kmpExtension: KotlinMultiplatformExtension,
    ) {
        // Configure common main
        val commonMain = kmpExtension.sourceSets.maybeCreate("commonMain")
        commonMain
            .kotlin
            .srcDir(project.layout.buildDirectory.dir("generated/uniffi/commonMain"))
        commonMain.dependencies {
            if (uniffiExtension.addRuntime.get()) {
                implementation("ch.ubique.uniffi:runtime:${Constants.RUNTIME_VERSION}")
            }

            implementation("com.squareup.okio:okio:${Constants.OKIO_VERSION}")
            implementation("org.jetbrains.kotlinx:atomicfu:${Constants.ATOMICFU_VERSION}")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Constants.COROUTINES_VERSION}")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Constants.DATETIME_VERSION}")
        }

        val targets = kmpExtension.targets
            .mapNotNull { target ->
                BuildTarget.fromTargetName(target.name)
                    ?.let { Pair(it, target) }
            }

        val namespace = uniffiExtension.bindingsGeneration.get().namespace.get()

        if (targets.any { (buildTarget, _) -> buildTarget in BuildTarget.nativeTargets }) {
            configureBaseNativeTarget(
                project,
                kmpExtension.sourceSets.maybeCreate("nativeMain"),
            )
        }

        // Configure all other build targets
        targets.forEach { (buildTarget, kotlinTarget) ->
            when (buildTarget) {
                BuildTarget.Jvm -> configureJvmTarget(
                    project,
                    kmpExtension.sourceSets.getByName("jvmMain")
                )

                BuildTarget.Android -> configureAndroidTarget(
                    project,
                    kmpExtension.sourceSets.getByName("androidMain"),
                    kmpExtension.sourceSets.maybeCreate("androidUnitTest")
                )

                in BuildTarget.nativeTargets -> configureNativeTarget(
                    project,
                    buildTarget,
                    kotlinTarget as KotlinNativeTarget,
                    namespace
                )

                else -> throw GradleException("Unhandled build target: ${buildTarget.name}")
            }
        }
    }

    private fun configureJvmTarget(
        project: Project,
        jvmMain: KotlinSourceSet
    ) {
        // Add generated bindings as a source directory
        jvmMain
            .kotlin
            .srcDir(project.layout.buildDirectory.dir("generated/uniffi/jvmMain"))

        // Add rust libraries to the resource path
        jvmMain
            .resources
            .srcDir(
                project
                    .layout
                    .buildDirectory
                    .dir("intermediates/rust/jvmMain/resources/${releaseString(isRelease)}")
            )

        // Add JNA dependencies
        jvmMain.dependencies {
            implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
        }

        val copyNativeLibsTask = project.tasks.named(
            copyNativeLibrariesTaskName(
                BuildTarget.Jvm,
                isRelease,
                dynamic = true
            )
        )

        // Hook into the JVM build process
        project.tasks.named("jvmProcessResources") {
            dependsOn(copyNativeLibsTask)
        }
    }

    private fun configureAndroidTarget(
        project: Project,
        androidMain: KotlinSourceSet,
        androidUnitTest: KotlinSourceSet,
    ) {
        // Add generated bindings as a source directory
        androidMain
            .kotlin
            .srcDir(project.layout.buildDirectory.dir("generated/uniffi/androidMain"))

        // Add aar version of JNA for android
        androidMain.dependencies {
            implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}@aar")
        }

        // Android unit tests run locally, so add the normal JNA dependency
        androidUnitTest.dependencies {
            implementation("net.java.dev.jna:jna:${Constants.JNA_VERSION}")
        }

        // For android builds, add rust libraries as to jni libs
        val androidExtension = project.extensions.getByType<AndroidExtension>()
        androidExtension
            .sourceSets
            .getByName("release")
            .jniLibs
            .setSrcDirs(listOf(project.layout.buildDirectory.dir("intermediates/rust/androidMain/jniLibs/Release")))
        androidExtension
            .sourceSets
            .getByName("debug")
            .jniLibs
            .setSrcDirs(listOf(project.layout.buildDirectory.dir("intermediates/rust/androidMain/jniLibs/Debug")))
        androidExtension
            .sourceSets
            .getByName("testRelease")
            .jniLibs
            .setSrcDirs(listOf(project.layout.buildDirectory.dir("intermediates/rust/androidMain/jniLibs/Release")))
        androidExtension
            .sourceSets
            .getByName("testDebug")
            .jniLibs
            .setSrcDirs(listOf(project.layout.buildDirectory.dir("intermediates/rust/androidMain/jniLibs/Debug")))

        // The "debug" source set is used for local tests, add it the native rust library
        // as a resource here (same as jvm).
        androidExtension
            .sourceSets
            .getByName("testDebug")
            .resources
            .srcDir(project.layout.buildDirectory.dir("intermediates/rust/androidMain/resources/Debug"))
        androidExtension
            .sourceSets
            .getByName("testRelease")
            .resources
            .srcDir(project.layout.buildDirectory.dir("intermediates/rust/androidMain/resources/Release"))


        // Hook into the Android build process
        project.tasks.withType<MergeSourceSetFolders> {
            val release = if (name.contains("release", ignoreCase = true)) {
                true
            } else if (name.contains("debug", ignoreCase = true)) {
                false
            } else {
                isRelease
            }

            val copyNativeLibsTask =
                copyNativeLibrariesTaskName(BuildTarget.Android, release, dynamic = true)
            inputs.dir(
                project.layout.buildDirectory.dir(
                    "intermediates/rust/androidMain/jniLibs/${
                        releaseString(
                            release
                        )
                    }"
                )
            )
            dependsOn(copyNativeLibsTask)
        }

        project.tasks.named("packageDebugResources") {
            dependsOn(
                copyNativeLibrariesTaskName(
                    BuildTarget.Android,
                    release = false,
                    dynamic = true
                )
            )
        }

        project.tasks.named("packageReleaseResources") {
            dependsOn(
                copyNativeLibrariesTaskName(
                    BuildTarget.Android,
                    release = true,
                    dynamic = true
                )
            )
        }
    }

    private fun configureBaseNativeTarget(
        project: Project,
        nativeMain: KotlinSourceSet,
    ) {
        nativeMain
            .kotlin
            .srcDir(project.layout.buildDirectory.dir("generated/uniffi/nativeMain"))

        // Dummy def file is the same for all native targets
        configureGenerateDummyDefFileTask(project)
    }

    private fun configureNativeTarget(
        project: Project,
        buildTarget: BuildTarget,
        nativeTarget: KotlinNativeTarget,
        namespace: String,
    ) {
        val generateDefFileTask = configureGenerateDefFileTask(project, buildTarget)
        val generatedDefFile = generateDefFileTask.map { it.outputFile }.get()

        val generateDummyDefFileTask =
            project.tasks.named<GenerateDummyDefFileTask>(GENERATE_DUMMY_DEF_FILE)
        val dummyDefFile = generateDummyDefFileTask.map { it.outputFile }.get()

        // As native targets need to be configured separately per architecture
        // there should be exactly one rust target for the build target.
        val rustTarget = buildTarget.checkedNativeTarget

        val copyNativeLibsTask = project.tasks.named(
            copyNativeLibrariesTaskName(
                rustTarget,
                buildTarget,
                isRelease,
                buildTarget.useDynamicLib == true
            ),
            Copy::class.java
        )
        val libraryIncludeDir = copyNativeLibsTask.get().destinationDir.path

        val isSync = project.gradle.startParameter.taskNames.isEmpty()

        nativeTarget.compilations.getByName("main") {
            cinterops.register("uniffi") {
                packageName("$namespace.cinterop")

                if (isSync) {
                    defFile(dummyDefFile)
                } else {
                    defFile(generatedDefFile)

                    extraOpts(
                        "-libraryPath",
                        libraryIncludeDir
                    )
                }

                project.tasks.named(interopProcessingTaskName) {
                    // Generates the headers file
                    dependsOn(BUILD_BINDINGS_TASK_NAME)

                    if (isSync) {
                        dependsOn(generateDummyDefFileTask)
                    } else {
                        // Generates the .def file
                        dependsOn(generateDefFileTask)
                        // Copy native libraries of this is not a sync
                        dependsOn(copyNativeLibsTask)
                    }
                }
            }
        }

        nativeTarget.compilerOptions {
            optIn.add("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    /**
     * Registers all possible build tasks without actually running them
     */
    private fun registerBuildTasks(project: Project) {
        // Register the build tasks for each rust target.
        // This task will run `cargo build --target <target>`
        // for the corresponding target.
        for (target in BuildTarget.RustTarget.entries) {
            for (release in listOf<Boolean>(true, false)) {
                val taskName = cargoBuildTaskName(target, release)
                project.tasks.register<CargoBuildTask>(taskName)
            }
        }

        // Once the libraries are built, they need to be referenced as a
        // resource in the JAR / APK, this task explicitly copies the libraries
        // into a separate folder, that is registered as a resource folder.
        for (buildTarget in BuildTarget.entries) {
            for (dynamic in listOf<Boolean>(true, false)) {
                // For debug targets
                buildTarget.debugTargets.forEach { rustTarget ->
                    val copyTaskName =
                        copyNativeLibrariesTaskName(rustTarget, buildTarget, false, dynamic)
                    project.tasks.register<Copy>(copyTaskName)
                }
                // For release targets
                buildTarget.releaseTargets.forEach { rustTarget ->
                    val copyTaskName =
                        copyNativeLibrariesTaskName(rustTarget, buildTarget, true, dynamic)
                    project.tasks.register<Copy>(copyTaskName)
                }

                // Create a unified task that copies all the libraries for the buildTarget (debug)
                project.tasks.register(copyNativeLibrariesTaskName(buildTarget, false, dynamic)) {
                    dependsOn(buildTarget.debugTargets.map {
                        copyNativeLibrariesTaskName(it, buildTarget, false, dynamic)
                    })
                }
                // Create a unified task that copies all the libraries for the buildTarget (release)
                project.tasks.register(copyNativeLibrariesTaskName(buildTarget, true, dynamic)) {
                    dependsOn(buildTarget.releaseTargets.map {
                        copyNativeLibrariesTaskName(it, buildTarget, true, dynamic)
                    })
                }
            }
        }
    }

    private fun configureBuildTasks(
        project: Project,
        rustSourceDir: DirectoryProperty,
    ) {
        val rustTargetDir =
            project.objects.directoryProperty().fileValue(File(cargoMetadata.targetDirectory))
        val buildOutputDir = project.layout.buildDirectory.dir("target")

        val androidExtension = project.extensions.findByType<AndroidExtension>()

        // Register the build tasks for each rust target.
        // This task will run `cargo build --target <target>`
        // for the corresponding target.
        for (target in BuildTarget.RustTarget.entries) {
            for (release in listOf<Boolean>(true, false)) {
                val taskName = cargoBuildTaskName(target, release)
                val cargoOutputDir = if (release) {
                    rustTargetDir.dir("${target.rustTriple}/release")
                } else {
                    rustTargetDir.dir("${target.rustTriple}/debug")
                }
                val outputDir = if (release) {
                    buildOutputDir.map { it.dir("${target.rustTriple}/release") }
                } else {
                    buildOutputDir.map { it.dir("${target.rustTriple}/debug") }
                }

                project.tasks.named<CargoBuildTask>(taskName) {
                    this.packageDirectory.set(rustSourceDir)
                    this.triple.set(target.rustTriple)
                    this.release.set(release)
                    this.packageName.set(targetPackage.name)
                    this.libraryName.set(this@UniffiPlugin.libraryName)
                    this.cargoOutputDirectory.set(cargoOutputDir)
                    this.outputDirectory.set(outputDir)

                    if (target.isAndroid && androidExtension != null) {
                        val sdkRoot = androidExtension.sdkDirectory
                        val apiLevel = androidExtension.defaultConfig.minSdk ?: 21
                        val ndkVersion = androidExtension.ndkVersion.takeIf(String::isNotEmpty)
                        val ndkRoot = androidExtension.ndkPath?.let { File(it) }

                        val ndkEnv = NdkUtil.ndkEnvVariables(
                            sdkRoot,
                            apiLevel,
                            ndkVersion,
                            ndkRoot,
                            target.rustTriple,
                            target.ndkLlvmTriple
                        )
                        this.additionalEnvironment.set(ndkEnv)
                    }
                }
            }
        }

        // Once the libraries are built, they need to be referenced as a
        // resource in the JAR / APK, this task explicitly copies the libraries
        // into a separate folder, that is registered as a resource folder.
        for (buildTarget in BuildTarget.entries) {
            for (dynamic in listOf<Boolean>(true, false)) {
                // For debug targets
                buildTarget.debugTargets.forEach { rustTarget ->
                    configureCopyNativeLibrariesTask(
                        project,
                        buildOutputDir,
                        buildTarget,
                        rustTarget,
                        false,
                        dynamic,
                        targetPackage.targets.first().name,
                    )
                }
                // For release targets
                buildTarget.releaseTargets.forEach { rustTarget ->
                    configureCopyNativeLibrariesTask(
                        project,
                        buildOutputDir,
                        buildTarget,
                        rustTarget,
                        true,
                        dynamic,
                        targetPackage.targets.first().name
                    )
                }
            }
        }
    }

    private fun configureCopyNativeLibrariesTask(
        project: Project,
        buildDir: Provider<Directory>,
        buildTarget: BuildTarget,
        rustTarget: BuildTarget.RustTarget,
        release: Boolean,
        dynamic: Boolean,
        packageName: String,
    ) {
        val cargoBuildTask = cargoBuildTaskName(rustTarget, release)
        val copyTaskName = copyNativeLibrariesTaskName(rustTarget, buildTarget, release, dynamic)

        val buildDir = if (release) {
            buildDir.map { it.dir("${rustTarget.rustTriple}/release") }
        } else {
            buildDir.map { it.dir("${rustTarget.rustTriple}/debug") }
        }
        val outputDir = if (rustTarget.apkLibraryPath != null) {
            project
                .layout
                .buildDirectory
                .dir(
                    "intermediates/rust/${buildTarget.sourceSetName}/jniLibs/${
                        releaseString(
                            release
                        )
                    }/${rustTarget.apkLibraryPath}"
                )
        } else {
            project
                .layout
                .buildDirectory
                .dir(
                    "intermediates/rust/${buildTarget.sourceSetName}/resources/${
                        releaseString(
                            release
                        )
                    }/${rustTarget.jarLibraryPath}"
                )
        }

        val libraryFileName = if (dynamic) {
            rustTarget.dynamicLibraryName(packageName)
        } else {
            rustTarget.staticLibraryName(packageName)
        } ?: throw GradleException("Could not determine library file name!")
        val libraryFile = buildDir.map { it.file(libraryFileName) }

        project.tasks.named<Copy>(copyTaskName) {
            from(libraryFile)
            into(outputDir)

            dependsOn(cargoBuildTask)
        }
    }

    /**
     * Registers the GenerateDefFileTask, needed for native targets.
     */
    private fun registerGenerateDefFileTask(project: Project) {
        for (buildTarget in BuildTarget.entries) {
            project.tasks.register<GenerateDefFileTask>(generateDefFileTaskName(buildTarget))
        }
        project.tasks.register<GenerateDummyDefFileTask>(GENERATE_DUMMY_DEF_FILE)
    }

    /**
     * Configures the GenerateDefFileTask, needed for native targets.
     */
    private fun configureGenerateDefFileTask(
        project: Project,
        buildTarget: BuildTarget,
    ): TaskProvider<GenerateDefFileTask> {
        val defFile = project
            .layout
            .buildDirectory
            .file("generated/uniffi/nativeInterop/cinterop/$libraryName-${buildTarget.name}.def")

        val rustTarget = buildTarget.checkedNativeTarget
        val staticLibName = rustTarget.staticLibraryName(libraryName)

        val headersDir = project
            .layout
            .buildDirectory
            .dir("generated/uniffi/nativeInterop/cinterop/headers/")

        val generateDefFileTask =
            project.tasks.named<GenerateDefFileTask>(generateDefFileTaskName(buildTarget)) {
                this.libraryName.set(staticLibName)
                this.outputFile.set(defFile)
                this.packageDirectory.set(cargoExtension.packageDirectory)
                this.targetString.set(rustTarget.rustTriple)
                this.headersDir.set(headersDir)


                dependsOn(BUILD_BINDINGS_TASK_NAME)
            }

        return generateDefFileTask
    }

    /**
     * Configures the GenerateDummyDefFileTask, needed to make syncs go fast.
     */
    private fun configureGenerateDummyDefFileTask(project: Project) {
        val dummyDefFile =
            project.layout.buildDirectory.file("generated/uniffi/nativeInterop/cinterop/dummy.def")

        val headersDir = project
            .layout
            .buildDirectory
            .dir("generated/uniffi/nativeInterop/cinterop/headers/")

        project.tasks.named<GenerateDummyDefFileTask>(GENERATE_DUMMY_DEF_FILE) {
            this.outputFile.set(dummyDefFile)
            this.headersDir.set(headersDir)

            dependsOn(BUILD_BINDINGS_TASK_NAME)
        }
    }

    private fun currentBuildVariant(project: Project): CargoBuildVariant? {
        val releaseBuildProperty = project.findProperty("releaseBuild")
        if (releaseBuildProperty == "true") return CargoBuildVariant.Release

        val taskNames = project.gradle.startParameter.taskNames

        // This environmental variable might be set by XCode
        val configuration = System.getenv("CONFIGURATION") ?: ""
        // NOTE: There is no better way of knowing what variant is currently built
        val isDebug = taskNames.any { it.contains("Debug", ignoreCase = true) }
                || configuration.contains("Debug", ignoreCase = true)
        val isRelease = taskNames.any { it.contains("Release", ignoreCase = true) }
                || configuration.contains("Release", ignoreCase = true)

        if (isRelease) return CargoBuildVariant.Release
        if (isDebug) return CargoBuildVariant.Debug
        return null
    }

    private fun releaseString(release: Boolean) = if (release) {
        "Release"
    } else {
        "Debug"
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
}