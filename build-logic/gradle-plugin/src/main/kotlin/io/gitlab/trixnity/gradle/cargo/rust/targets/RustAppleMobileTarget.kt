/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.rust.targets

import io.gitlab.trixnity.gradle.cargo.rust.CrateType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.Serializable

/**
 * Represents a Rust Apple mobile target.
 */
enum class RustAppleMobileTarget(
    override val rustTriple: String,
    override val cinteropName: String,
) : RustMobileTarget, RustNativeTarget, Serializable {
    // TODO: Add watchOS and tvOS targets
    IosArm64(
        rustTriple = "aarch64-apple-ios",
        cinteropName = "ios",
    ),
    IosSimulatorArm64(
        rustTriple = "aarch64-apple-ios-sim",
        cinteropName = "ios",
    ),
    IosX64(
        rustTriple = "x86_64-apple-ios",
        cinteropName = "ios",
    );

    override val friendlyName = name

    override val supportedKotlinPlatformTypes = arrayOf(KotlinPlatformType.native)
    override fun outputFileName(crateName: String, crateType: CrateType): String? =
        crateType.outputFileNameForMacOS(crateName)

    override fun toString() = rustTriple
}
