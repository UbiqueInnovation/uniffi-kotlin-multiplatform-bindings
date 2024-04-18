/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.rust.targets

import io.gitlab.trixnity.gradle.cargo.rust.CrateType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.Serializable

enum class RustWindowsTarget(
    override val rustTriple: String,
    override val jnaResourcePrefix: String,
) : RustJvmTarget, Serializable {
    X64(
        rustTriple = "x86_64-pc-windows-msvc",
        jnaResourcePrefix = "win32-x86-64",
    ),
    Arm64(
        rustTriple = "aarch64-pc-windows-msvc",
        jnaResourcePrefix = "win32-aarch64",
    );

    override val friendlyName: String = "Windows$name"

    override val supportedKotlinPlatformTypes = arrayOf(KotlinPlatformType.jvm)
    override fun outputFileName(crateName: String, crateType: CrateType): String? =
        crateType.outputFileNameForMsvc(crateName)

    override fun toString() = rustTriple
}
