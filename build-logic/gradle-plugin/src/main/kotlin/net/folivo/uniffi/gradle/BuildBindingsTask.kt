package net.folivo.uniffi.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class BuildBindingsTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bindgen: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val udlFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val libraryFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val libraryName: Property<String>

    @get:Input
    abstract val crateName: Property<String>

    @TaskAction
    fun buildBindings(): Unit = with(project) {
        exec { spec ->
            spec.commandLine(
                bindgen.get(),
                "--lib-file",
                libraryFile.get(),
                "--out-dir",
                outputDirectory.get(),
                "--crate",
                crateName.get(),
                udlFile.get()
            )
        }.assertNormalExitValue()

        val defFilePath = outputDirectory.get().file("nativeInterop/cinterop/${crateName.get()}.def")
        val defFileFile = defFilePath.asFile
        defFileFile.parentFile.mkdirs()
        defFileFile.writeText("staticLibraries = lib${libraryName.get()}.a\n")
    }
}
