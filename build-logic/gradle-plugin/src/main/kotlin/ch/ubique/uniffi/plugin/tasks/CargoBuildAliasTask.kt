package ch.ubique.uniffi.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.DisableCachingByDefault

/**
 * Compatibility task for the historic `buildLibraryForBindings` task name.
 *
 * The input is wired to the real Cargo task's output. Gradle therefore schedules the Cargo task
 * automatically when this alias is requested, without a manually maintained dependency edge.
 */
@DisableCachingByDefault(because = "This compatibility alias only validates the Cargo output")
abstract class CargoBuildAliasTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val library: RegularFileProperty
}
