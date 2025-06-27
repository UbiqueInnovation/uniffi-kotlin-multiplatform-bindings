package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.utils.CargoRunner
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import kotlin.String

abstract class GenerateDefFileTask : DefaultTask() {
    @get:Input
    abstract val libraryName: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Internal
    abstract val packageDirectory: DirectoryProperty

    @get:Input
    abstract val targetString: Property<String>

    @get:InputDirectory
    abstract val headersDir: DirectoryProperty

    @TaskAction
    fun generateDefFile() {
        val output = outputFile.get().asFile

        val libraryName = libraryName.get()

        val allHeaders = headersDir.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension == "h" }
            .toList()
            .joinToString(" ")

        output.writeText(
            """
            staticLibraries = $libraryName
            headers = $allHeaders
            """.trimIndent()
        )

        val opts = getLinkerOpts()
        if (opts != null) {
            output.appendText("\nlinkerOpts = $opts")
        }
    }

    private fun getLinkerOpts(): String? {
        val output = CargoRunner(logger) {
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