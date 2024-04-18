/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import io.gitlab.trixnity.gradle.Variant
import io.gitlab.trixnity.gradle.cargo.rust.targets.RustAndroidTarget
import io.gitlab.trixnity.gradle.cargo.tasks.FindDynamicLibrariesTask
import io.gitlab.trixnity.gradle.utils.register
import org.gradle.api.Project
import javax.inject.Inject

abstract class CargoAndroidBuildVariant @Inject constructor(
    project: Project,
    build: CargoAndroidBuild,
    variant: Variant,
    extension: CargoExtension,
) : DefaultCargoBuildVariant<RustAndroidTarget, CargoAndroidBuild>(project, build, variant, extension),
    CargoMobileBuildVariant<RustAndroidTarget>, HasAndroidProperties {
    init {
        @Suppress("LeakingThis") ndkLibraries.addAll(build.ndkLibraries)
    }

    val findNdkLibrariesTaskProvider = project.tasks.register<FindDynamicLibrariesTask>({
        +this@CargoAndroidBuildVariant
    }) {
        rustTarget.set(this@CargoAndroidBuildVariant.rustTarget)
        libraryNames.set(this@CargoAndroidBuildVariant.ndkLibraries)
    }
}
