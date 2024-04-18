/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.rust.profiles

/**
 * Represents a Cargo profile.
 */
sealed interface CargoProfile {
    /**
     * The name of the profile.
     */
    val profileName: String

    /**
     * The name of the directory under the target directory where the output will be placed.
     */
    val targetChildDirectoryName: String

    companion object {
        val Dev: CargoProfile = BuiltInCargoProfile.Dev
        val Release: CargoProfile = BuiltInCargoProfile.Release
        val Test: CargoProfile = BuiltInCargoProfile.Test
        val Bench: CargoProfile = BuiltInCargoProfile.Bench
    }
}

fun CargoProfile(profileName: String): CargoProfile =
    BuiltInCargoProfile.entries.firstOrNull { it.profileName == profileName }
        ?: CustomCargoProfile(profileName)
