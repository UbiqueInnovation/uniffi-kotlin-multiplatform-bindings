/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import org.gradle.api.provider.SetProperty

interface HasFeatures {
    /**
     * The names of the Cargo features to use. Defaults to an empty set. The list of features for a build is determined
     * by merging the `features` properties in all blocks corresponding to the build. For example, in the following DSL:
     * ```kotlin
     * cargo {
     *   features += "logging"
     *   builds.android {
     *     features += "android"
     *     debug {
     *       features += "debug"
     *     }
     *   }
     * }
     * ```
     * the feature set `{"logging", "android", "debug"}` will be used for Android debug builds.
     */
    val features: SetProperty<String>
}
