/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.rust.profiles

import java.io.Serializable

/**
 * Represents a Cargo profile defined by the user.
 */
internal data class CustomCargoProfile(
    override val profileName: String
) : CargoProfile, Serializable {
    override val targetChildDirectoryName: String = profileName
    override fun toString(): String = profileName
}
