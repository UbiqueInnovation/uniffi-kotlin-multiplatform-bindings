package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.model.CargoMetadata
import ch.ubique.uniffi.plugin.utils.CargoRunner
import ch.ubique.uniffi.plugin.utils.targetPackage
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class BuildBindingsTask : DefaultTask() {

    @get:Internal
    abstract val packageDirectory: DirectoryProperty

    @get:InputFiles
    val rustSources: FileTree
        get() = packageDirectory.get().asFileTree.matching {
            exclude("build")
            include("**/*.rs")
            include("Cargo.toml", "Cargo.lock")
        }

    @get:InputFile
    abstract val bindgen: RegularFileProperty

    @get:InputFile
	@get:Optional
    abstract val libraryFile: RegularFileProperty
	@get:InputFile
	@get:Optional
	abstract val udlFile: RegularFileProperty


    @get:Input
    abstract val cargoMetadata: Property<String>

	@get:Input
	abstract val generateBindingsForExternalCrates: Property<Boolean>

    /**
     * Where bindgen is pointed with `--out-dir`. Not declared as the task's output:
     * see [generatedDirectories].
     */
    @get:Internal
    val bindingsDirectory = project.layout.buildDirectory.dir("generated/uniffi")

    /**
     * The generated sources, declared one directory per source set rather than as the
     * whole `generated/uniffi` tree.
     *
     * `com.android.kotlin.multiplatform.library` derives AGP's own source directories
     * as *siblings* of every registered Kotlin source directory. Because the plugin
     * registers `generated/uniffi/<sourceSet>` as a Kotlin srcDir, AGP ends up with
     * inputs such as `generated/uniffi/baselineProfiles` (and `res`, `assets`, ...).
     * Declaring the shared parent as this task's output therefore made every one of
     * those an undeclared consumer of this task's output:
     *
     *     Task ':runtime:prepareAndroidMainArtProfile' uses this output of task
     *     ':runtime:buildBindings' without declaring an explicit or implicit dependency
     *
     * Declaring the per-source-set directories instead leaves AGP's derived siblings
     * outside this task's outputs, which is also a more honest description of what
     * bindgen actually writes.
     */
    @get:OutputDirectories
    val generatedDirectories: List<Provider<Directory>>
        get() = GENERATED_SOURCE_SETS.map { sourceSet ->
            bindingsDirectory.map { it.dir(sourceSet) }
        }

    private companion object {
        /** The sub directories bindgen emits into, see `--out-dir`. */
        val GENERATED_SOURCE_SETS = listOf(
            "commonMain",
            "jvmMain",
            "androidMain",
            "nativeMain",
            "nativeInterop",
        )
    }

//    @OutputDirectory
//    val bindingsDirectory = project.layout.buildDirectory.dir("generated/uniffi")

    @TaskAction
    fun action() {
        val metadata = CargoMetadata.fromJsonString(cargoMetadata.get())

        val targetPackage = metadata.targetPackage

        buildBindings(targetPackage.targets[0].name)
    }

    private fun buildBindings(crateName: String) {
		val command = if(udlFile.isPresent) {
			mutableListOf(
				bindgen.get().asFile.path,
				udlFile.get().asFile.path,
				"--out-dir",
				bindingsDirectory.get().asFile.path,
			)
		} else if (libraryFile.isPresent) {
			mutableListOf(
				bindgen.get().asFile.path,
				"--library",
				libraryFile.get().asFile.path,
				"--out-dir",
				bindingsDirectory.get().asFile.path,
			)
		} else {
			throw RuntimeException("neither library file nor udl file was provided")
		}
		// If it shouldn't generate bindings for external crates specify the '--crate' argument
		if (!generateBindingsForExternalCrates.get()) {
			command.add("--crate")
			command.add(crateName)
		}
        val process = ProcessBuilder(command)
            .directory(packageDirectory.asFile.get())
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        check(exitCode == 0) {
            println(output)
            "Failed to generate bindings with exit code $exitCode"
        }
    }
}