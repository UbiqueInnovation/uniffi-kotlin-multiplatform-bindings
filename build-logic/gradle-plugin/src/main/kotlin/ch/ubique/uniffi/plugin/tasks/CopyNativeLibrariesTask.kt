package ch.ubique.uniffi.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class CopyNativeLibrariesTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val libraryFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    // `project.copy { }` reaches for Project at execution time, which the
    // configuration cache forbids. FileSystemOperations is the injected equivalent.
    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun doCopy() {
        fileSystemOperations.copy {
            from(libraryFile)
            into(outputDir)
        }
    }

//    @TaskAction
//    fun doCopy() {
//        project.copy {
//            from(libraryFile)
//            into(outputDir)
//        }
//    }
}
