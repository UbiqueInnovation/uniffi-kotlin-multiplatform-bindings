package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.model.CrateType
import ch.ubique.uniffi.plugin.utils.CargoRunner
import ch.ubique.uniffi.plugin.utils.PRINT_NATIVE_STATIC_LIBS
import ch.ubique.uniffi.plugin.utils.nativeStaticLibs
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

/**
 * Asks rustc for the linker options of the static library's native dependencies, for a crate built
 * in [workspace mode][ch.ubique.uniffi.plugin.dsl.BuildMode.Workspace]. The workspace build can't
 * pass rustc arguments, so this builds the crate on its own with
 * `cargo rustc --crate-type staticlib -- --print native-static-libs`.
 *
 * That build compiles the crate's workspace dependencies with the features of this crate alone.
 * A dependency declaring a `cdylib` shares its place in Cargo's cache with the workspace build, so
 * the next workspace build compiles it again. The task therefore only depends on what decides the
 * native dependencies, the workspace's manifests and lock file, and not on the library: it runs on
 * the first build and when the dependencies change, not on every build.
 */
@DisableCachingByDefault(because = "The rust toolchain is not a declared input")
abstract class ProbeNativeStaticLibsTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    /**
     * The output of `cargo metadata` for the package, which covers the manifests of all workspace
     * members: their dependencies, features and crate types.
     */
    @get:Input
    abstract val cargoMetadata: Property<String>

    /** The workspace's `Cargo.lock`, for the resolved dependencies. Missing before the first build. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val lockFile: ConfigurableFileCollection

    @get:Internal
    abstract val packageDirectory: DirectoryProperty

    @get:Input
    val packageDirectoryPath: String
        get() = packageDirectory.get().asFile.absolutePath

    /** Cargo's shared target directory. */
    @get:Internal
    abstract val cargoTargetDirectory: DirectoryProperty

    @get:Input
    abstract val targetString: Property<String>

    /** The file name of the static library Cargo writes, which the probe must not replace. */
    @get:Input
    abstract val libraryFileName: Property<String>

    /** The features that apply to the package built on its own. */
    @get:Input
    abstract val features: SetProperty<String>

    @get:Input
    abstract val useCross: Property<Boolean>

    /** Optional rustc wrapper, for example `sccache`. */
    @get:Input
    @get:Optional
    abstract val rustcWrapper: Property<String>

    /** Optional workspace rustc wrapper, for example `sccache`. */
    @get:Input
    @get:Optional
    abstract val rustcWorkspaceWrapper: Property<String>

    @get:Input
    abstract val additionalEnvironment: MapProperty<String, String>

    /** The linker options, as rustc reports them. Empty if it reports none. */
    @get:OutputFile
    abstract val nativeStaticLibsFile: RegularFileProperty

    @TaskAction
    fun probe() {
        nativeStaticLibsFile.get().asFile.writeText(keepingLibrary(::runProbe).orEmpty())
    }

    /**
     * Runs [probe] with the library Cargo leaves for the debug profile moved aside. Cargo puts the
     * probe's library there, built with other crate types and possibly with other features than
     * the workspace build's. Without moving it back, cinterop would get the probe's library.
     */
    private fun <T> keepingLibrary(probe: () -> T): T {
        val library = cargoTargetDirectory.get().asFile.resolve("${targetString.get()}/debug/${libraryFileName.get()}")
        if (!library.isFile) return probe()
        val kept = File(library.parentFile, "${library.name}.uniffi-kept")
        Files.move(library.toPath(), kept.toPath(), StandardCopyOption.REPLACE_EXISTING)
        try {
            return probe()
        } finally {
            Files.move(kept.toPath(), library.toPath(), StandardCopyOption.ATOMIC_MOVE)
        }
    }

    private fun runProbe(): String? {
        val output = CargoRunner(execOperations, logger, useCross = useCross.get()) {
            argument("rustc")
            argument("--lib")
            argument("--target")
            argument(targetString.get())
            // Cargo fingerprints the extra rustc arguments below, so the probe needs its own
            // fingerprint. A staticlib alone gets one, while a crate type set that includes a cdylib
            // would share it with the workspace build, and the two would keep rebuilding each other.
            argument("--crate-type")
            argument(CrateType.SystemStaticLibrary.toString())
            if (features.get().isNotEmpty()) {
                argument("--features")
                argument(features.get().sorted().joinToString(","))
            }
            argument("--")
            PRINT_NATIVE_STATIC_LIBS.forEach(::argument)

            workdir(packageDirectory.asFile.get())

            env("CARGO_TARGET_DIR", cargoTargetDirectory.get().asFile.absolutePath)
            rustcWrapper.orNull?.let { env("RUSTC_WRAPPER", it) }
            rustcWorkspaceWrapper.orNull?.let { env("RUSTC_WORKSPACE_WRAPPER", it) }
            additionalEnvironment.get().forEach { (key, value) ->
                env(key, value)
            }

            redirectErrorStream(true)
        }.run()

        return nativeStaticLibs(output)
    }
}
