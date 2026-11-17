package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.dsl.BuildMode
import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.model.CrateType
import ch.ubique.uniffi.plugin.services.CargoWorkspaceService
import ch.ubique.uniffi.plugin.utils.CargoRunner
import ch.ubique.uniffi.plugin.utils.PRINT_NATIVE_STATIC_LIBS
import ch.ubique.uniffi.plugin.utils.nativeStaticLibs
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "Cargo caches incrementally, and the rust toolchain is not a declared input")
abstract class CargoBuildTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Internal
    abstract val packageDirectory: DirectoryProperty

    @get:Input
    val packageDirectoryPath: String
        get() = packageDirectory.get().asFile.absolutePath

    /**
     * The rust target to build for.
     *
     * Left unset for a plain host build, which is what the bindings library needs: cargo
     * is then invoked without `--target` and writes to `<targetDirectory>/<profile>`
     * rather than `<targetDirectory>/<triple>/<profile>`.
     */
    @get:Optional
    @get:Input
    abstract val rustTarget: Property<BuildTarget.RustTarget>

    @get:Input
    abstract val release: Property<Boolean>

    @get:Internal
    val profile: Provider<String> = release.map { if (it) "release" else "debug" }

    @get:Input
    abstract val additionalEnvironment: MapProperty<String, String>

    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val libraryName: Property<String>

    @get:Input
    abstract val useCross: Property<Boolean>

    /** Whether the crate is built alone or together with the workspace, see [BuildMode]. */
    @get:Input
    abstract val buildMode: Property<BuildMode>

    /** The system crate types required by the consuming Kotlin targets. */
    @get:Input
    @get:Optional
    abstract val crateTypes: SetProperty<CrateType>

    /**
     * The crate types declared in the package's `[lib] crate-type`, as reported by `cargo metadata`.
     * In [BuildMode.Workspace] they must include [crateTypes]: `cargo build` can't add a crate type,
     * and `cargo rustc` only adds one to a single package.
     */
    @get:Input
    @get:Optional
    abstract val declaredCrateTypes: SetProperty<CrateType>

    /**
     * The workspace crates the package depends on, directly or through other workspace crates,
     * that declare a `cdylib`. [BuildMode.Individual] warns if there are any.
     *
     * Before 1.100, Cargo fingerprints a package declaring a `cdylib` in a single slot per target.
     * When it is compiled as a dependency of several packages, with different features or as the
     * root of its own `cargo rustc --crate-type` build, these builds keep replacing each other's
     * fingerprint, and the package and everything depending on it is rebuilt on every invocation.
     */
    @get:Input
    @get:Optional
    abstract val cdylibDependencies: SetProperty<String>

    /**
     * The packages built together in [BuildMode.Workspace]: those of every project in the workspace
     * that builds this Rust target in that mode.
     */
    @get:Input
    @get:Optional
    abstract val workspacePackageNames: SetProperty<String>

    /** Where Cargo runs when it builds the [workspacePackageNames], i.e. the workspace root. */
    @get:Internal
    abstract val workspaceDirectory: DirectoryProperty

    /** Runs the workspace build once for all projects that request it. */
    @get:Internal
    abstract val workspaceService: Property<CargoWorkspaceService>

    /**
     * Cargo features to enable, passed as `--features`. In [BuildMode.Workspace], these are the
     * features of every project building the workspace, qualified with their package.
     */
    @get:Input
    abstract val features: SetProperty<String>

    /** The dependencies of [packageName] by their name in its `Cargo.toml`. */
    @get:Input
    @get:Optional
    abstract val dependencyNames: SetProperty<String>

    /**
     * The [features] for a build of [packageName] on its own, which the def file probe uses as
     * well. In [BuildMode.Workspace], only the features of [packageName] and of its dependencies
     * are kept: Cargo rejects any other when it builds a single package.
     */
    @get:Internal
    val packageFeatures: Provider<List<String>>
        get() = features.zip(buildMode) { features, mode ->
            if (mode == BuildMode.Individual) {
                features.filter(String::isNotBlank).sorted()
            } else {
                val packages = dependencyNames.getOrElse(emptySet()) + packageName.get()
                features.filter { it.substringBefore('/').removeSuffix("?") in packages }.sorted()
            }
        }

    /** Optional rustc wrapper, for example `sccache`. */
    @get:Input
    @get:Optional
    abstract val rustcWrapper: Property<String>

    /** Optional workspace rustc wrapper, for example `sccache`. */
    @get:Input
    @get:Optional
    abstract val rustcWorkspaceWrapper: Property<String>

    /**
     * Cargo's own target directory, i.e. `CARGO_TARGET_DIR` / `target-dir` as reported by
     * `cargo metadata`. The per triple and per profile subdirectories below it are
     * cargo's layout, so they are derived here instead of by the plugin.
     */
    @get:Internal
    abstract val cargoTargetDirectory: DirectoryProperty

    /** Where cargo places this build's artifacts. */
    @get:Internal
    val cargoOutputDirectory: Provider<Directory> = cargoTargetDirectory.dir(
        rustTarget.map { "${it.rustTriple}/" }
            .orElse("")
            .zip(profile) { triple, profile -> "$triple$profile" }
    )

    /** The dynamic library emitted directly into Cargo's shared output directory. */
    @get:OutputFile
    @get:Optional
    abstract val dynamicLibraryFile: RegularFileProperty

    /** The static library emitted directly into Cargo's shared output directory. */
    @get:OutputFile
    @get:Optional
    abstract val staticLibraryFile: RegularFileProperty

    /**
     * Where a [BuildMode.Individual] build of a static library writes the linker options for its
     * native dependencies, as rustc's `--print native-static-libs` reports them. Usually
     * `<library name>.native-static-libs` next to the library, like for
     * [prebuilt libraries][ch.ubique.uniffi.plugin.dsl.CargoExtension.prebuiltLibraries]. A
     * workspace build can't pass rustc arguments, so the def file task probes them instead.
     */
    @get:OutputFile
    @get:Optional
    abstract val nativeStaticLibsFile: RegularFileProperty

    init {
        // Cargo checks its complete dependency graph and toolchain state on every invocation.
        outputs.upToDateWhen { false }
        dynamicLibraryFile.set(libraryFile(BuildTarget.RustTarget::dynamicLibraryName))
        staticLibraryFile.set(libraryFile(BuildTarget.RustTarget::staticLibraryName))
    }

    private fun libraryFile(
        fileName: (BuildTarget.RustTarget, String) -> String?,
    ): Provider<RegularFile> = cargoOutputDirectory.zip(
        rustTarget.orElse(BuildTarget.RustTarget.forCurrentPlatform)
            .zip(libraryName) { target, library ->
                fileName(target, library)
                    ?: throw GradleException("Could not determine library file name for $target")
            }
    ) { directory, library -> directory.file(library) }

    @TaskAction
    fun build() {
        when (buildMode.get()) {
            BuildMode.Workspace -> buildWithWorkspace()
            BuildMode.Individual -> buildIndividually()
        }
    }

    /**
     * Runs `cargo build` for [packageName] and the [workspacePackageNames] from the workspace root.
     * Every project building the workspace runs the same command, so the [workspaceService] runs it
     * once for all of them.
     */
    private fun buildWithWorkspace() {
        val missing = crateTypes.getOrElse(emptySet()) - declaredCrateTypes.getOrElse(emptySet())
        if (missing.isNotEmpty()) {
            val types = missing.map(CrateType::toString).sorted().joinToString(", ")
            throw GradleException(
                "The Kotlin targets of ${packageName.get()} need crate-type $types, missing from " +
                    "[lib] crate-type in its Cargo.toml. Add it there, or build the crate on its own " +
                    "with cargo { buildMode = BuildMode.Individual }."
            )
        }
        val cargo = cargo(
            subcommand = "build",
            workdir = workspaceDirectory.get().asFile,
            packages = (workspacePackageNames.getOrElse(emptySet()) + packageName.get()).sorted(),
            features = features.get().filter(String::isNotBlank).sorted(),
        )
        workspaceService.get().runOnce(cargo.invocation) { cargo.run() }
    }

    /** Runs `cargo rustc` for [packageName] alone, with exactly the [crateTypes] it needs. */
    private fun buildIndividually() {
        val cdylibs = cdylibDependencies.getOrElse(emptySet())
        if (cdylibs.isNotEmpty()) {
            logger.warn(
                "${packageName.get()} depends on workspace crates declaring crate-type cdylib: " +
                    "${cdylibs.sorted().joinToString(", ")}. Cargo before 1.100 rebuilds them and " +
                    "everything depending on them on every build. Use Cargo 1.100 or newer, remove " +
                    "cdylib from their [lib] crate-type, or build the workspace together with " +
                    "cargo { buildMode = BuildMode.Workspace }."
            )
        }
        val nativeStaticLibsFile = nativeStaticLibsFile.orNull?.asFile
        val output = cargo(
            subcommand = "rustc",
            workdir = packageDirectory.get().asFile,
            packages = listOf(packageName.get()),
            crateTypes = crateTypes.getOrElse(emptySet()).map(CrateType::toString).sorted(),
            features = packageFeatures.get(),
            rustcArguments = if (nativeStaticLibsFile != null) PRINT_NATIVE_STATIC_LIBS else emptyList(),
        ).run()
        nativeStaticLibsFile?.writeText(nativeStaticLibs(output).orEmpty())
    }

    private fun cargo(
        subcommand: String,
        workdir: File,
        packages: List<String>,
        crateTypes: List<String> = emptyList(),
        features: List<String>,
        rustcArguments: List<String> = emptyList(),
    ) = CargoRunner(execOperations, logger, useCross = useCross.get()) {
        argument(subcommand)
        argument("--lib")
        rustTarget.orNull?.let {
            argument("--target")
            argument(it.rustTriple)
        }
        packages.forEach {
            argument("--package")
            argument(it)
        }
        if (release.get()) {
            argument("--release")
        }
        if (crateTypes.isNotEmpty()) {
            argument("--crate-type")
            argument(crateTypes.joinToString(","))
        }
        if (features.isNotEmpty()) {
            argument("--features")
            argument(features.joinToString(","))
        }
        if (rustcArguments.isNotEmpty()) {
            argument("--")
            rustcArguments.forEach(::argument)
            // rustc reports on stderr.
            redirectErrorStream(true)
        }

        workdir(workdir)

        env("CARGO_TARGET_DIR", cargoTargetDirectory.get().asFile.absolutePath)
        rustcWrapper.orNull?.let { env("RUSTC_WRAPPER", it) }
        rustcWorkspaceWrapper.orNull?.let { env("RUSTC_WORKSPACE_WRAPPER", it) }

        additionalEnvironment.get().forEach { (key, value) ->
            env(key, value)
        }
    }
}
