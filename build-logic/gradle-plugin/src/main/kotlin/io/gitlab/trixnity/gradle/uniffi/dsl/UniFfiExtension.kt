/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.uniffi.dsl

import io.gitlab.trixnity.gradle.Variant
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

abstract class UniFfiExtension(internal val project: Project) {
    /**
     * The bindgen crate path.
     * This is only necessary if you want to build with a local bindgen source crate.
     */
    abstract val bindgenCratePath: DirectoryProperty

    internal abstract val bindingsGeneration: Property<BindingsGeneration>

    fun generateFromUdl(configure: Action<BindingsGenerationFromUdl> = Action { }) {
        val generation = bindingsGeneration.orNull ?: project.objects.newInstance<BindingsGenerationFromUdl>(project)
            .also { bindingsGeneration.set(it) }

        generation as? BindingsGenerationFromUdl
            ?: throw GradleException("A `generateFromLibrary` block has already been defined.")


        configure.execute(generation)
    }

    fun generateFromLibrary(configure: Action<BindingsGenerationFromLibrary> = Action { }) {
        val generation =
            bindingsGeneration.orNull ?: project.objects.newInstance<BindingsGenerationFromLibrary>(project)
                .also { bindingsGeneration.set(it) }

        generation as? BindingsGenerationFromLibrary
            ?: throw GradleException("A `generateFromUdl` block has already been defined.")

        configure.execute(generation)
    }
}

sealed class BindingsGeneration(internal val project: Project) {
    /**
     * The UDL namespace. Defaults to `"$libraryCrateName"`.
     */
    abstract val namespace: Property<String>

    /**
     * The name of the build to use to generate bindings. If unspecified, one of the available builds will be
     * automatically selected.
     */
    abstract val build: Property<String>

    /**
     * The variant of the build to use to generate bindings. If unspecified, one of the available variants will be
     * automatically selected.
     */
    abstract val variant: Property<Variant>
}

abstract class BindingsGenerationFromUdl @Inject internal constructor(project: Project) : BindingsGeneration(project) {
    /**
     * The UDL file. Defaults to `"${crateDirectory}/src/${crateName}.udl"`.
     */
    abstract val udlFile: RegularFileProperty

    /**
     * Path to the optional uniffi config file.
     * If not provided, uniffi-bindgen will try to guess it from the UDL's file location.
     */
    abstract val config: RegularFileProperty
}

abstract class BindingsGenerationFromLibrary @Inject internal constructor(project: Project) :
    BindingsGeneration(project)
