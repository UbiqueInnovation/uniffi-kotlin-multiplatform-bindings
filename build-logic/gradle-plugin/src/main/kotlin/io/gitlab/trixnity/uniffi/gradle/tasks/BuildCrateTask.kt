/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

@CacheableTask
abstract class BuildCrateTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val crateDirectory: DirectoryProperty

    @get:Input
    abstract val libraryName: Property<String>

    @get:Input
    abstract val profile: Property<String>

    @get:Input
    abstract val crateTypes: SetProperty<String>

    @get:Internal
    abstract val targetDirectory: DirectoryProperty

    @Suppress("LeakingThis")
    @OutputFiles
    val libraryFileByCrateType: Provider<Map<String, RegularFile>> =
        targetDirectory.zip(crateTypes) { targetDirectory, crateTypes ->
            crateTypes.mapNotNull { crateType ->
                val libraryFileName = buildLibraryFileName(libraryName.get(), crateType) ?: return@mapNotNull null
                crateType to targetDirectory.file(libraryFileName)
            }.toMap()
        }

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

fun buildLibraryFileName(libraryName: String, crateType: String): String? {
    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    return when {
        os.isLinux -> when (crateType) {
            "staticlib" -> "lib$libraryName.a"
            "cdylib" -> "lib$libraryName.so"
            else -> null
        }

        os.isMacOsX -> when (crateType) {
            "staticlib" -> "lib$libraryName.a"
            "cdylib" -> "lib$libraryName.dylib"
            else -> null
        }

        os.isWindows -> when (crateType) {
            "staticlib" -> "$libraryName.lib"
            "cdylib" -> "$libraryName.dll"
            else -> null
        }

        else -> throw GradleException("Unsupported operating system: ${os.displayName}")
    }
}
