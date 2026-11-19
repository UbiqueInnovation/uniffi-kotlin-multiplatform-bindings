package ch.ubique.uniffi.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import kotlin.String

@DisableCachingByDefault(because = "Generating the def file is cheaper than fetching it from the build cache")
abstract class GenerateDummyDefFileTask : DefaultTask() {
    @get:Input
    val constant: String = "v1"

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val headersDir: DirectoryProperty

    @TaskAction
    fun generateDefFile() {
        val output = outputFile.get().asFile

        val allHeaders = headersDir.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension == "h" }
            .toList()
            .joinToString(" ")

        if (!output.exists()) {
            output.parentFile.mkdirs()
            output.writeText(
                """
                headers = $allHeaders
                compilerOpts = -I${headersDir.get().asFile.path}
                """.trimIndent()
            )
        }
    }
}