package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.utils.CargoRunner
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.mapProperty
import java.io.File
import kotlin.String

abstract class CargoBuildTask : DefaultTask() {

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

    @get:Input
    abstract val triple: Property<String>

    @get:Input
    abstract val release: Property<Boolean>

    @Input
    val additionalEnvironment: MapProperty<String, String> =
        project.objects.mapProperty<String, String>().convention(emptyMap<String, String>())

    @get:Input
    abstract val packageName: Property<String>

    @get:Internal
    abstract val cargoOutputDirectory: DirectoryProperty

    @get:Input
    abstract val libraryName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun build() {
        CargoRunner(logger) {
            argument("build")
            argument("--target")
            argument(triple.get())
            argument("--package")
            argument(packageName.get())

            if (release.get()) {
                argument("--release")
            }

            workdir(packageDirectory.asFile.get())

            additionalEnvironment.get().forEach { key, value ->
                env(key, value)
            }
        }.run()

        val targetDir = outputDirectory.asFile.get()
        targetDir.mkdirs()

        cargoOutputDirectory.get()
            .asFile
            .listFiles { file -> file.name.contains(libraryName.get()) }
            .forEach { file -> file.copyTo(File(targetDir, file.name), overwrite = true) }
    }
}