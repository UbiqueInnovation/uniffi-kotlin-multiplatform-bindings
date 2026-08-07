package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.utils.CargoRunner
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Cargo caches incrementally, and the rust toolchain is not a declared input")
abstract class CargoBuildTask : DefaultTask() {

    @get:Internal
    abstract val packageDirectory: DirectoryProperty

    @get:Input
    val packageDirectoryPath: String
        get() = packageDirectory.get().asFile.absolutePath

    @get:InputFiles
    val rustSources: FileTree
        get() = packageDirectory.get().asFileTree.matching { pattern ->
            pattern.exclude("build")
            pattern.include("**/*.rs")
            pattern.include("Cargo.toml", "Cargo.lock")
        }

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

    /** Where this task collects them, which is the layout the rest of the build sees. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    val dynamicLibraryFile: Provider<RegularFile> =
        libraryFile(BuildTarget.RustTarget::dynamicLibraryName)

    @get:Internal
    val staticLibraryFile: Provider<RegularFile> =
        libraryFile(BuildTarget.RustTarget::staticLibraryName)

    private fun libraryFile(
        fileName: (BuildTarget.RustTarget, String) -> String?,
    ): Provider<RegularFile> = outputDirectory.file(
        rustTarget.orElse(BuildTarget.RustTarget.forCurrentPlatform)
            .zip(libraryName) { target, library ->
                fileName(target, library)
                    ?: throw GradleException("Could not determine library file name for $target")
            }
    )

    @TaskAction
    fun build() {
        CargoRunner(logger, useCross = useCross.get()) {
            argument("build")
            if (rustTarget.isPresent) {
                argument("--target")
                argument(rustTarget.get().rustTriple)
            }
            argument("--package")
            argument(packageName.get())

            if (release.get()) {
                argument("--release")
            }

            workdir(packageDirectory.asFile.get())

            additionalEnvironment.get().forEach { (key, value) ->
                env(key, value)
            }
        }.run()

        val targetDir = outputDirectory.asFile.get()
        targetDir.mkdirs()

        cargoOutputDirectory.get()
            .asFile
            .listFiles { file -> file.name.contains(libraryName.get()) }
            .forEach { file -> file.copyTo(File(targetDir, file.name), overwrite = true) }
    }
}
