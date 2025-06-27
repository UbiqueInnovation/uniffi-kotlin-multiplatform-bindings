package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.model.CargoMetadata
import ch.ubique.uniffi.plugin.utils.CargoRunner
import ch.ubique.uniffi.plugin.utils.targetPackage
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class BuildBindingsTask : DefaultTask() {

    @get:Internal
    abstract val packageDirectory: DirectoryProperty

    @get:InputFiles
    val rustSources: FileTree
        get() = packageDirectory.get().asFileTree.matching {
            exclude("build")
            include("**/*.rs")
            include("Cargo.toml", "Cargo.lock")
        }

    @get:InputFile
    abstract val bindgen: RegularFileProperty

    @get:InputFile
    abstract val libraryFile: RegularFileProperty

    @get:Input
    abstract val cargoMetadata: Property<String>

    @OutputDirectory
    val bindingsDirectory = project.layout.buildDirectory.dir("generated/uniffi")

    @TaskAction
    fun action() {
        val metadata = CargoMetadata.fromJsonString(cargoMetadata.get())

        val targetPackage = metadata.targetPackage

        buildBindings(targetPackage.targets[0].name)
    }

    private fun buildBindings(crateName: String) {
        val process = ProcessBuilder(
            bindgen.get().asFile.path,
            "--library",
            libraryFile.get().asFile.path,
            "--out-dir",
            bindingsDirectory.get().asFile.path,
            "--crate",
            crateName
        )
            .directory(packageDirectory.asFile.get())
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        check(exitCode == 0) {
            println(output)
            "Failed to generate bindings with exit code $exitCode"
        }
    }
}