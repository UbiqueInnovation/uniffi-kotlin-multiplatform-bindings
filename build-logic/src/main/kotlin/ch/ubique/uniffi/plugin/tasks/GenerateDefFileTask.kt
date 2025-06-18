package ch.ubique.uniffi.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import kotlin.String

abstract class GenerateDefFileTask : DefaultTask() {
    @get:InputFile
    abstract val headers: RegularFileProperty

    @get:Input
    abstract val libraryName: Property<String>

    @get:Input
    abstract val includeStaticLib: Property<Boolean>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generateDefFile() {
        val output = outputFile.get().asFile

        val headersPath = headers.get().asFile.absolutePath

        if (includeStaticLib.get()) {
            val libraryName = libraryName.get()

            output.writeText("""
            staticLibraries = $libraryName
            headers = $headersPath
            """.trimIndent())
        } else {
            output.writeText("""
            headers = $headersPath
            """.trimIndent())
        }
    }
}