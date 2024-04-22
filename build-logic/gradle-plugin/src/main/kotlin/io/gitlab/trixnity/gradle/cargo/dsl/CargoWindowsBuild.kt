/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import io.gitlab.trixnity.gradle.CargoHost
import io.gitlab.trixnity.gradle.cargo.rust.targets.RustWindowsTarget
import org.gradle.api.Project
import javax.inject.Inject

/**
 * Contains settings for Rust builds for Windows.
 */
@Suppress("LeakingThis")
abstract class CargoWindowsBuild @Inject constructor(
    project: Project,
    rustTarget: RustWindowsTarget,
    extension: CargoExtension,
) : DefaultCargoBuild<RustWindowsTarget, CargoWindowsBuildVariant>(
    project,
    rustTarget,
    extension,
    CargoWindowsBuildVariant::class,
), CargoJvmBuild<CargoWindowsBuildVariant> {
    init {
        resourcePrefix.convention(rustTarget.jnaResourcePrefix)
        jvm.convention(true)
        androidUnitTest.convention(rustTarget == CargoHost.current.hostTarget)
    }
}
