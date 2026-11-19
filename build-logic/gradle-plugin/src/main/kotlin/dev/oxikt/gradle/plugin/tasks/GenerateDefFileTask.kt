package ch.ubique.uniffi.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Generating the def file is cheaper than fetching it from the build cache")
abstract class GenerateDefFileTask : DefaultTask() {
    /**
     * The static library cinterop links against.
     *
     * This is the [ch.ubique.uniffi.plugin.tasks.CargoBuildTask] output rather than a name,
     * so it does three things at once: it names the library, it locates it (both end up in
     * the def file), and it makes this task - and through the def file, cinterop - depend on
     * the cargo build.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val staticLibrary: RegularFileProperty

    /**
     * The linker options for the library's native dependencies, as rustc's
     * `--print native-static-libs` reports them: next to a prebuilt library, written by an
     * individual Cargo build, or by a [ProbeNativeStaticLibsTask] for a workspace build.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val nativeStaticLibsFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val targetString: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val headersDir: DirectoryProperty

    @TaskAction
    fun generateDefFile() {
        val output = outputFile.get().asFile

        val library = staticLibrary.get().asFile

        val allHeaders = headersDir.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension == "h" }
            .toList()
            .joinToString(" ")

        // `libraryPaths` goes in here rather than being passed to cinterop as
        // `-libraryPath`: extraOpts wants a plain string during configuration, which would
        // mean guessing the cargo build's output location instead of reading it off the task.
        output.writeText(
            """
			staticLibraries = ${library.name}
			libraryPaths = ${library.parentFile.path}
			headers = $allHeaders
			compilerOpts = -I${headersDir.get().asFile.path}
			""".trimIndent()
        )

        val nativeStaticLibs = nativeStaticLibsFile.get().asFile.readText().trim().ifEmpty { null }
        val opts = listOfNotNull(nativeStaticLibs, duplicateSymbolOpt())
        if (opts.isNotEmpty()) {
            output.appendText("\nlinkerOpts = ${opts.joinToString(" ")}")
        }
    }

    private fun duplicateSymbolOpt(): String? {
        val target = targetString.get()
        return when {
            // Kotlin/Native drives the mingw link through clang++, so the flag needs forwarding.
            target.contains("windows") -> "-Wl,--allow-multiple-definition"
            // ld.lld is invoked directly for linux targets.
            target.contains("linux") -> "--allow-multiple-definition"
            // Apple's ld64 already resolves duplicates this way.
            else -> null
        }
    }
}
