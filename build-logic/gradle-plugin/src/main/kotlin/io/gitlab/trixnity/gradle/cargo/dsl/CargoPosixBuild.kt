/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import io.gitlab.trixnity.gradle.RustHost
import io.gitlab.trixnity.gradle.cargo.rust.targets.RustPosixTarget
import org.gradle.api.Project
import javax.inject.Inject

/**
 * Contains settings for Rust builds for mobile platforms.
 */
@Suppress("LeakingThis")
abstract class CargoPosixBuild @Inject constructor(
    project: Project,
    rustTarget: RustPosixTarget,
    extension: CargoExtension,
) : DefaultCargoBuild<RustPosixTarget, CargoPosixBuildVariant>(
    project,
    rustTarget,
    extension,
    CargoPosixBuildVariant::class,
), CargoJvmBuild<CargoPosixBuildVariant>, CargoNativeBuild<CargoPosixBuildVariant> {
    init {
        resourcePrefix.convention(rustTarget.jnaResourcePrefix)
        jvm.convention(true)
        androidUnitTest.convention(rustTarget == RustHost.current.rustTarget)
    }
}
