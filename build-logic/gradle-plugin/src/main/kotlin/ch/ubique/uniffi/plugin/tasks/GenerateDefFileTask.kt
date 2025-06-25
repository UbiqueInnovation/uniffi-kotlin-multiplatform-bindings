package ch.ubique.uniffi.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
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

    @TaskAction
    fun generateDefFile() {
        val output = outputFile.get().asFile

        val libraryName = libraryName.get()

        output.writeText("""
            staticLibraries = $libraryName
            """.trimIndent())

        val opts = getLinkerOpts()
        if (opts != null) {
            output.appendText("\nlinkerOpts = $opts")
        }
    }

    private fun getLinkerOpts(): String? {
        val process = ProcessBuilder(
            "cargo",
            "rustc",
            "--target",
            targetString.get(),
            "--",
            "--print",
            "native-static-libs"
        )
            .directory(packageDirectory.asFile.get())
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        check(exitCode == 0) {
            println(output)
            "Failed to build the rust project with exit code $exitCode"
        }

        val linkerFlag = output.split('\n')
            .map { it.trim().substringAfter("note: native-static-libs: ", "") }.firstOrNull(String::isNotEmpty)

        return linkerFlag
    }
}