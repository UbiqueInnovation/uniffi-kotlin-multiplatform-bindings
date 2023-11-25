/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

@CacheableTask
abstract class BuildCrateTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val crateDirectory: DirectoryProperty

    @get:Input
    abstract val crateName: Property<String>

    @get:Input
    abstract val profile: Property<String>

    @get:Internal
    abstract val targetDirectory: DirectoryProperty

    @Suppress("LeakingThis")
    @get:OutputFile
    val libraryFile: RegularFileProperty = project.objects.fileProperty().convention(
        targetDirectory.map { it.file(buildLibraryName(crateName.get())) },
    )

    @TaskAction
    fun buildBindings(): Unit = with(project) {
        exec { spec ->
            spec.workingDir(crateDirectory)
            spec.commandLine("cargo", "build")
            if (profile.get() != "debug") {
                spec.args("--profile", profile.get())
            }
        }.assertNormalExitValue()
    }
}

fun buildLibraryName(crateName: String): String {
    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    return when {
        os.isLinux -> "lib$crateName.so"
        os.isMacOsX -> "lib$crateName.dylib"
        os.isWindows -> "$crateName.dll"
        else -> throw GradleException("Unsupported operating system: ${os.displayName}")
    }
}
