package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.model.CargoMetadata
import ch.ubique.uniffi.plugin.services.CargoMetadataService
import ch.ubique.uniffi.plugin.utils.targetPackage
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class BuildBindingsTask : DefaultTask() {
    @get:InputDirectory
    abstract val packageDirectory: DirectoryProperty

    @get:InputFile
    abstract val bindgen: RegularFileProperty

    @get:Internal
    abstract val cargoMetadataService: Property<CargoMetadataService>

    @OutputDirectory
    val bindingsDirectory = project.layout.buildDirectory.dir("generated/uniffi")

    @TaskAction
    fun action() {
        val metadata = cargoMetadataService.get().getMetadata(packageDirectory.get().asFile)

        val targetPackage = metadata.targetPackage

        cargoBuild(targetPackage.name)

        val libraryPath = getPathToLibrary(metadata, targetPackage)

        buildBindings(libraryPath, targetPackage.targets[0].name)
    }

    private fun cargoBuild(packageName: String) {
        // TODO: Allow specification of target
        val process = ProcessBuilder(
            "cargo",
            "build",
            "--package",
            packageName,
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