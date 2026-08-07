package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.model.CargoMetadata
import ch.ubique.uniffi.plugin.utils.targetPackage
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

// Caching this would be worthwhile - bindgen is slow and the bindings are small text
// files - but only once every input is declared: `uniffi.toml` is currently not part of
// [rustSources], so a cache hit could hand out bindings generated with a different
// configuration.
@DisableCachingByDefault(because = "Not every input that affects the bindings is declared yet")
abstract class BuildBindingsTask : DefaultTask() {

    @get:Internal
    abstract val packageDirectory: DirectoryProperty

    @get:InputFiles
    val rustSources: FileTree
        get() = packageDirectory.get().asFileTree.matching { pattern ->
            pattern.exclude("build")
            pattern.include("**/*.rs")
            pattern.include("Cargo.toml", "Cargo.lock")
            pattern.include("uniffi.toml")
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

    @get:Internal
    abstract val bindingsDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val commonMainDir: DirectoryProperty

    @get:OutputDirectory
    abstract val jvmMainDir: DirectoryProperty

    @get:OutputDirectory
    abstract val androidMainDir: DirectoryProperty

    @get:OutputDirectory
    abstract val nativeMainDir: DirectoryProperty

    @get:OutputDirectory
    abstract val nativeInteropDir: DirectoryProperty

    @get:OutputDirectory
    abstract val nativeInteropHeadersDir: DirectoryProperty

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    init {
        // Need to be set like that, otherwise the generation dependency is not preserved
        commonMainDir.convention(bindingsDirectory.dir("commonMain"))
        jvmMainDir.convention(bindingsDirectory.dir("jvmMain"))
        androidMainDir.convention(bindingsDirectory.dir("androidMain"))
        nativeMainDir.convention(bindingsDirectory.dir("nativeMain"))
        nativeInteropDir.convention(bindingsDirectory.dir("nativeInterop"))
        nativeInteropHeadersDir.convention(bindingsDirectory.dir("nativeInterop/cinterop/headers"))
    }

    @TaskAction
    fun action() {
        fileSystemOperations.delete { spec ->
            spec.delete(bindingsDirectory)
        }

        val outputDirectory = bindingsDirectory.get().asFile
        outputDirectory.mkdirs()

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