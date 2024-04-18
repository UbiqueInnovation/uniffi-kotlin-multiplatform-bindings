/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.rust.profiles

/**
 * The profiles provided by Cargo by default.
 */
internal enum class BuiltInCargoProfile(
    override val profileName: String,
    override val targetChildDirectoryName: String,
) : CargoProfile {
    Dev("dev", "debug"),
    Release("release", "release"),
    Test("test", "debug"),
    Bench("bench", "release");

    override fun toString(): String = profileName
}
