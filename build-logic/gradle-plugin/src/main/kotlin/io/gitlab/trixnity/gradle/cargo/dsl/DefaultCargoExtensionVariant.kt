/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import io.gitlab.trixnity.gradle.Variant
import io.gitlab.trixnity.gradle.cargo.rust.profiles.CargoProfile
import org.gradle.api.Project
import javax.inject.Inject

internal abstract class DefaultCargoExtensionVariant @Inject constructor(
    final override val project: Project,
    final override val variant: Variant,
    extension: CargoExtension
) : CargoExtensionVariant {
    init {
        @Suppress("LeakingThis")
        profile.convention(
            when (variant) {
                Variant.Debug -> CargoProfile.Dev
                Variant.Release -> CargoProfile.Release
            }
        )

        @Suppress("LeakingThis")
        features.addAll(extension.features)
    }
}
