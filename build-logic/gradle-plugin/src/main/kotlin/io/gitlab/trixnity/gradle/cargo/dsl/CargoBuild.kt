/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import io.gitlab.trixnity.gradle.cargo.rust.targets.RustTarget
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectCollection
import org.jetbrains.kotlin.gradle.plugin.HasProject
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

/**
 * Contains settings for Rust builds.
 */
interface CargoBuild<out CargoBuildVariantT : CargoBuildVariant<RustTarget>>
    : Named, HasProject, HasFeatures, HasVariants<CargoBuildVariantT> {
    /**
     * The Rust target triple to use to build the current target. The name of the `CargoBuild` is
     * `rustTarget.friendlyName`.
     */
    val rustTarget: RustTarget

    /**
     * The list of Kotlin targets requiring this Rust build.
     */
    val kotlinTargets: NamedDomainObjectCollection<KotlinTarget>
}
