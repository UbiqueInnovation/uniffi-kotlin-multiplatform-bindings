package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.utils.CargoRunner
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
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
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val staticLibrary: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Internal
    abstract val packageDirectory: DirectoryProperty

    /** Cargo's shared target directory, also used by the linker-options probe below. */
    @get:Internal
    abstract val cargoTargetDirectory: DirectoryProperty

    @get:Input
    abstract val targetString: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val headersDir: DirectoryProperty

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

    /** Additional environment variables passed to the Cargo linker-options probe. */
    @get:Input
    abstract val additionalEnvironment: MapProperty<String, String>

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

        val opts = listOfNotNull(getLinkerOpts(), duplicateSymbolOpt())
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

    private fun getLinkerOpts(): String? {
        val output = CargoRunner(logger, useCross = useCross.get()) {
            argument("rustc")
            argument("--lib")
            argument("--target")
            argument(targetString.get())
            argument("--crate-type")
            argument("staticlib")
            argument("--")
            argument("--print")
            argument("native-static-libs")

            workdir(packageDirectory.asFile.get())

            env("CARGO_TARGET_DIR", cargoTargetDirectory.get().asFile.absolutePath)
            rustcWrapper.orNull?.let { env("RUSTC_WRAPPER", it) }
            rustcWorkspaceWrapper.orNull?.let { env("RUSTC_WORKSPACE_WRAPPER", it) }
            additionalEnvironment.get().forEach { (key, value) ->
                env(key, value)
            }

            redirectErrorStream(true)
        }.run()

        val linkerFlag = output.split('\n')
            .map { it.trim().substringAfter("note: native-static-libs: ", "") }
            .firstOrNull(String::isNotEmpty)

        return linkerFlag
    }
}
