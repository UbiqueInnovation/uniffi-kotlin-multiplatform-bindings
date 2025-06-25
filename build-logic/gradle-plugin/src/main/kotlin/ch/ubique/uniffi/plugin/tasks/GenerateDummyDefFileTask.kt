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

abstract class GenerateDummyDefFileTask : DefaultTask() {
    @get:Input
    val constant: String = "v1"

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generateDefFile() {
        val output = outputFile.get().asFile
        if (!output.exists()) {
            output.parentFile.mkdirs()
            output.writeText("")
        }
    }
}