package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.utils.CargoRunner
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import kotlin.String

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
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val staticLibrary: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Internal
    abstract val packageDirectory: DirectoryProperty

    @get:Input
    abstract val targetString: Property<String>

    @get:InputDirectory
    abstract val headersDir: DirectoryProperty

    @get:Input
    abstract val useCross: Property<Boolean>

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

        val opts = getLinkerOpts()
        if (opts != null) {
            output.appendText("\nlinkerOpts = $opts")
        }
    }

    private fun getLinkerOpts(): String? {
        val output = CargoRunner(logger, useCross = useCross.get()) {
            argument("rustc")
            argument("--target")
            argument(targetString.get())
            argument("--")
            argument("--print")
            argument("native-static-libs")

            workdir(packageDirectory.asFile.get())

            redirectErrorStream(true)
        }.run()

        val linkerFlag = output.split('\n')
            .map { it.trim().substringAfter("note: native-static-libs: ", "") }
            .firstOrNull(String::isNotEmpty)

        return linkerFlag
    }
}