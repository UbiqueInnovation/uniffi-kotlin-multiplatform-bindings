/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.gradle

import io.gitlab.trixnity.uniffi.gradle.utils.CargoPackage
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import javax.inject.*

abstract class UniFfiExtension constructor(internal val project: Project) {
    /**
     * The bindgen crate path.
     * This is only necessary if you want to build with a local bindgen source crate.
     */
    abstract val bindgenCratePath: DirectoryProperty

    internal abstract val bindingsGeneration: Property<BindingsGeneration>

    fun generateFromUdl(configure: Action<BindingsGenerationFromUdl>) {
        val generation = bindingsGeneration.orNull
            ?: project.objects.newInstance<BindingsGenerationFromUdl>(project).also { bindingsGeneration.set(it) }

        generation as? BindingsGenerationFromUdl
            ?: throw GradleException("A `generateFromLibrary` block has already been defined.")


        configure.execute(generation)
    }

    fun generateFromLibrary(configure: Action<BindingsGenerationFromLibrary>) {
        val generation = bindingsGeneration.orNull
            ?: project.objects.newInstance<BindingsGenerationFromLibrary>(project).also { bindingsGeneration.set(it) }

        generation as? BindingsGenerationFromLibrary
            ?: throw GradleException("A `generateFromUdl` block has already been defined.")

        configure.execute(generation)
    }
}

abstract class BindingsGeneration internal constructor(internal val project: Project) {
    /**
     * The crate directory.
     */
    abstract val crateDirectory: DirectoryProperty

    @Suppress("LeakingThis")
    val cargoPackage: Provider<CargoPackage> = crateDirectory.map { CargoPackage(it) }

    /**
     * The package name, as defined in Cargo.toml.
     */
    val packageName: Provider<String> = cargoPackage.map { it.name }

    /**
     * The library name, as defined in Cargo.toml, or the crate name with dashes replaced with underscores.
     */
    val libraryName: Provider<String> = cargoPackage.map { it.libraryName }

    /**
     * The crate types to be generated, as defined in Cargo.toml.
     */
    val crateTypes: Provider<Set<String>> = cargoPackage.map { it.crateTypes }

    /**
     * The UDL namespace. Defaults to `"${libraryName}"`.
     */
    val namespace: Property<String> = project.objects.property<String>().convention(libraryName)

    /**
     * The crate build profile. Defaults to `"debug"`.
     */
    val profile: Property<String> = project.objects.property<String>().convention("debug")
}

abstract class BindingsGenerationFromUdl @Inject internal constructor(project: Project) :
    BindingsGeneration(project) {
    /**
     * The UDL file. Defaults to `"${crateDirectory}/src/${crateName}.udl"`.
     */
    @Suppress("LeakingThis")
    val udlFile: RegularFileProperty = project.objects.fileProperty().convention(
        crateDirectory.map { it.file("src/${libraryName.get()}.udl") }
    )

    /**
     * Path to the optional uniffi config file.
     * If not provided, uniffi-bindgen will try to guess it from the UDL's file location.
     */
    abstract val config: RegularFileProperty
}

abstract class BindingsGenerationFromLibrary @Inject internal constructor(project: Project) :
    BindingsGeneration(project)
