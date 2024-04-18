/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import io.gitlab.trixnity.gradle.cargo.rust.profiles.CargoProfile
import org.gradle.api.provider.Property

interface HasProfile {
    /**
     * The Cargo profile to use in this variant. When unspecified, this defaults to `CargoProfile.Dev` for debug builds
     * and `CargoProfile.Release` for release builds. Setting this will override the `profile` properties in outer
     * blocks. For example, in the following DSL:
     * ```kotlin
     * cargo {
     *   release {
     *     profile = CargoProfile("optimize-speed")
     *   }
     *   builds.mobile {
     *     release {
     *       profile = CargoProfile("minimize-size")
     *     }
     *   }
     * }
     * ```
     * iOS and Android builds will invoke `cargo build --profile minimize-size`, while the others will invoke
     * `cargo build --profile optimize-size`.
     */
    val profile: Property<CargoProfile>
}
