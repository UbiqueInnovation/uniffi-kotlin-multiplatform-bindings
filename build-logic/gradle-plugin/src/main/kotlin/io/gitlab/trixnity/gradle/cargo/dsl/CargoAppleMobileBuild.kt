/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import io.gitlab.trixnity.gradle.cargo.rust.targets.RustAppleMobileTarget
import org.gradle.api.Project
import javax.inject.Inject

/**
 * Contains settings for Rust builds for Apple mobile platforms.
 */
abstract class CargoAppleMobileBuild @Inject constructor(
    project: Project,
    rustTarget: RustAppleMobileTarget,
    extension: CargoExtension,
) : DefaultCargoBuild<RustAppleMobileTarget, CargoAppleMobileBuildVariant>(
    project,
    rustTarget,
    extension,
    CargoAppleMobileBuildVariant::class,
), CargoMobileBuild<CargoAppleMobileBuildVariant>,
    CargoNativeBuild<CargoAppleMobileBuildVariant>
