package ch.ubique.uniffi.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Merges several per-architecture library directories into a single tree, keeping
 * each source directory's own name as the sub directory in the output.
 *
 * This exists because AGP's `SourceDirectories.addGeneratedSourceDirectory` wires
 * *one* task to *one* directory, while rust produces one library per ABI. The
 * individual [CopyNativeLibrariesTask]s stay per-ABI (jvm and the native targets
 * consume them directly); this task only regroups them for android, which needs
 *
 *     <root>/<abi>/lib<name>.so          (jniLibs)
 *     <root>/<jarLibraryPath>/lib<name>  (host test resources, same layout as jvm)
 *
 * NOTE: the sub directory name is taken from the source directory's own name, so
 * the callers must point this at directories that are already named after the ABI
 * (or the JNA `jarLibraryPath`). See `UniffiPlugin.registerCopyNativeLibrariesTask`,
 * which is what produces them.
 *
 * The output directory is deliberately *not* set by the plugin for the android
 * wiring: `addGeneratedSourceDirectory` assigns it.
 */
abstract class MergeNativeLibrariesTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirectories: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun merge() {
        val output = outputDirectory.get()

        // Stale ABIs must not survive a target list change.
        fileSystemOperations.delete { spec ->
            spec.delete(output.asFile.listFiles().orEmpty())
        }

        sourceDirectories.files
            .filter { it.isDirectory }
            .forEach { directory ->
                fileSystemOperations.copy { spec ->
                    spec.from(directory)
                    spec.into(output.dir(directory.name))
                }
            }
    }
}
