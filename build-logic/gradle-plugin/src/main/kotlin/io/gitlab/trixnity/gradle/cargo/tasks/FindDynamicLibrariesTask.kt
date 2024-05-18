/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.tasks

import io.gitlab.trixnity.gradle.cargo.rust.CrateType
import io.gitlab.trixnity.gradle.cargo.rust.targets.RustTarget
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class FindDynamicLibrariesTask : DefaultTask() {
    @get:Input
    abstract val rustTarget: Property<RustTarget>

    @get:Input
    abstract val libraryNames: SetProperty<String>

    @get:Input
    abstract val searchPaths: ListProperty<File>

    @get:OutputFile
    abstract val libraryPathsCacheFile: RegularFileProperty

    @Suppress("LeakingThis")
    @Internal
    val libraryPaths: Provider<Set<File>> = libraryPathsCacheFile.map {
        it.asFile.readText().split(' ').filter(String::isNotEmpty).map(::File).toSet()
    }

    @get:Inject
    abstract val projectLayout: ProjectLayout

    @TaskAction
    fun copyDynamicLibraries() {
        val rustTarget = rustTarget.get()
        val searchPaths = searchPaths.get() + projectLayout.projectDirectory.asFile

        val libraryPaths = libraryNames.get().mapNotNull { libraryName ->
            val libraryPath = File(libraryName)
            if (libraryPath.isAbsolute) {
                return@mapNotNull libraryPath.takeIf(File::exists)
            }

            searchPaths.firstNotNullOfOrNull { searchPath ->
                searchPath
                    .resolve(rustTarget.outputFileName(libraryName, CrateType.SystemDynamicLibrary)!!)
                    .takeIf(File::exists)
                    ?: searchPath
                        .resolve(libraryName)
                        .takeIf(File::exists)
            }
        }

        libraryPathsCacheFile.get().asFile.run {
            parentFile.mkdirs()
            writeText(libraryPaths.joinToString(" ") { it.absolutePath })
        }
    }
}
