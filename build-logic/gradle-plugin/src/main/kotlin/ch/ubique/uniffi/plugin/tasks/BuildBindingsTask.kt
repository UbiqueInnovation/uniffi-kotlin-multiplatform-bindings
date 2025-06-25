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

    @get:Input
    val packageDirectoryPath: String
        get() = packageDirectory.get().asFile.absolutePath

    @get:InputFiles
    val rustSources: FileTree
        get() = packageDirectory.get().asFileTree.matching {
            exclude("build")
            include("**/*.rs")
            include("Cargo.toml", "Cargo.lock")
        }

    @get:InputFile
    abstract val bindgen: RegularFileProperty

    @get:Input
    abstract val cargoMetadata: Property<String>

    @OutputDirectory
    val bindingsDirectory = project.layout.buildDirectory.dir("generated/uniffi")

    @TaskAction
    fun action() {
        val metadata = CargoMetadata.fromJsonString(cargoMetadata.get())

        val targetPackage = metadata.targetPackage

        cargoBuild(targetPackage.name)

        val libraryPath = getPathToLibrary(metadata, targetPackage)

        buildBindings(libraryPath, targetPackage.targets[0].name)
    }

    private fun cargoBuild(packageName: String) {
        // TODO: Allow specification of target
        CargoRunner(logger) {
            argument("build")
            argument("--package")
            argument(packageName)
        }.run()
    }


    private fun getPathToLibrary(
        metadata: CargoMetadata,
        targetPackage: CargoMetadata.Package
    ): File {
        val targetLib = targetPackage.targets.first()

        val crateName = targetLib.name

        // TODO: Make sure this works cross platform
        val libraryName =
            targetLib.kinds.first { it.isNormalLibrary() }.crateType.outputFileNameForMacOS(
                crateName
            )

        return File("${metadata.targetDirectory}/debug/$libraryName")
    }

    private fun buildBindings(libraryPath: File, crateName: String) {
        val process = ProcessBuilder(
            bindgen.get().asFile.path,
            "--library",
            libraryPath.path,
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