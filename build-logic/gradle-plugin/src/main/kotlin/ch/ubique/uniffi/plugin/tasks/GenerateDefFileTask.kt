package ch.ubique.uniffi.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import kotlin.String

abstract class GenerateDefFileTask : DefaultTask() {
    @get:InputDirectory
    abstract val headers: DirectoryProperty

    @get:Input
    abstract val libraryName: Property<String>

    @get:Input
    abstract val includeStaticLib: Property<Boolean>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generateDefFile() {
        val output = outputFile.get().asFile

        val headersDir = headers.asFile.get()
        val allHeaders = headersDir.walkTopDown()
            .filter { it.isFile && it.extension == "h" }
            .joinToString(" ") { it.absolutePath }

        if (includeStaticLib.get()) {
            val libraryName = libraryName.get()

            output.writeText("""
            staticLibraries = $libraryName
            headers = $allHeaders
            """.trimIndent())
        } else {
            output.writeText("""
            headers = $allHeaders
            """.trimIndent())
        }
    }
}