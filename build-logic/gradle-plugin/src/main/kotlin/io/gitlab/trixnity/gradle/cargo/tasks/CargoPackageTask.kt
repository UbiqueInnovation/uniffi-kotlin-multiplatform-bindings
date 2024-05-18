/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.tasks

import io.gitlab.trixnity.gradle.cargo.CargoPackage
import io.gitlab.trixnity.gradle.utils.CommandSpec
import org.gradle.api.file.Directory
import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import java.io.File
import javax.inject.Inject

abstract class CargoPackageTask : CargoTask() {
    @get:Internal
    abstract val cargoPackage: Property<CargoPackage>

    @Suppress("LeakingThis")
    @get:Internal
    val root: Provider<Directory> = cargoPackage.map { it.root }

    @Suppress("LeakingThis", "unused")
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val manifestFile: Provider<RegularFile> = cargoPackage.map { it.manifestFile }

    @Suppress("LeakingThis", "unused")
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val lockFile: Provider<RegularFile> = cargoPackage.map { it.lockFile }

    /**
     * Rust source files of all library and build script targets of all resolved Cargo dependencies. This includes
     * the current package's source files as well. This is to make Gradle re-invoke the command when any Rust source
     * file is changed.
     */
    @Suppress("LeakingThis", "unused")
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    val dependencySourceFiles: Provider<List<File>> = cargoPackage.map { cargoPackage ->
        cargoPackage.dependencyTargetRoots.flatMap { targetRoot ->
            targetRoot.parentFile.walk()
                .onEnter { it.name !in arrayOf("target", "build", ".gradle", ".idea") }
                .filter { it.extension == "rs" }
        }
    }

    override fun configureFromProperties(spec: CommandSpec) {
        super.configureFromProperties(spec)
        spec.workingDirectory(cargoPackage.map { it.root })
    }
}
