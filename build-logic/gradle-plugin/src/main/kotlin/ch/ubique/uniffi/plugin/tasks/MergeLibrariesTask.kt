package ch.ubique.uniffi.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/**
 * Collects the per rust target libraries into a single tree, one subdirectory per
 * entry:
 *
 *     <root>/<abi>/lib<name>.so          (android jniLibs)
 *     <root>/<jarLibraryPath>/lib<name>  (jvm resources, android host test resources)
 *
 * This exists because both AGP's `SourceDirectories.addGeneratedSourceDirectory` and
 * JNA's classpath lookup want *one* root, while rust produces one library per target.
 *
 * The output directory is deliberately left unset by the plugin for the android wiring:
 * `addGeneratedSourceDirectory` assigns it.
 */
@DisableCachingByDefault(because = "Copying the libraries is cheaper than fetching them from the build cache")
abstract class MergeLibrariesTask : DefaultTask() {

    /** One library (or tree of libraries) and the subdirectory it is placed in. */
    interface NativeLibrary {
        @get:Input
        val directoryName: Property<String>

        @get:InputFiles
        @get:PathSensitive(PathSensitivity.ABSOLUTE)
        val files: ConfigurableFileCollection
    }

    @get:Nested
    abstract val libraries: ListProperty<NativeLibrary>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val objects: ObjectFactory

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    fun library(directoryName: String, files: Any) {
        libraries.add(
            objects.newInstance(NativeLibrary::class.java).apply {
                this.directoryName.set(directoryName)
                this.files.from(files)
            }
        )
    }

    @TaskAction
    fun merge() {
        // sync, not copy: a target dropped from the list must not survive in the output.
        fileSystemOperations.sync { spec ->
            spec.into(outputDirectory)

            libraries.get().forEach { library ->
                spec.into(library.directoryName.get()) { child ->
                    child.from(library.files)
                }
            }
        }
    }
}
