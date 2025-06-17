package ch.ubique.uniffi.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.mapProperty
import kotlin.String

abstract class CargoBuildTask : DefaultTask() {

    @get:InputDirectory
    abstract val packageDirectory: DirectoryProperty

    @get:Input
    abstract val triple: Property<String>

    @get:Input
    abstract val release: Property<Boolean>

    @Input
    val additionalEnvironment: MapProperty<String, String>
        = project.objects.mapProperty<String, String>().convention(emptyMap<String, String>())

    @get:Input
    abstract val packageName: Property<String>

    // @get:OutputDirectory
    // abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun build() {
        val arguments = mutableListOf<String>(
            "cargo",
            "build",
            "--target",
            triple.get(),
            "--package",
            packageName.get()
        )

        if (release.get()) {
            arguments += "--release"
        }

        val processBuilder = ProcessBuilder(arguments)
        processBuilder.redirectErrorStream(true)

        val env = processBuilder.environment()
        additionalEnvironment.get().forEach { key, value ->
            env[key] = value
        }

        val process = processBuilder.start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        check(exitCode == 0) {
            println(output)
            "Failed build rust code with exit code $exitCode"
        }
    }
}