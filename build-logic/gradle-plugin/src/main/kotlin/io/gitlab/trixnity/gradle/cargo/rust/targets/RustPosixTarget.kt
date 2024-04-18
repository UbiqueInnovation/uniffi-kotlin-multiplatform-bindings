/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.rust.targets

import io.gitlab.trixnity.gradle.cargo.rust.CrateType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.Serializable

enum class RustPosixTarget(
    override val rustTriple: String,
    override val jnaResourcePrefix: String,
    override val cinteropName: String,
) : RustJvmTarget, RustNativeTarget, Serializable {
    MinGWX64(
        rustTriple = "x86_64-pc-windows-gnu",
        jnaResourcePrefix = "win32-x86-64",
        cinteropName = "mingw",
    ),
    MacOSX64(
        rustTriple = "x86_64-apple-darwin",
        jnaResourcePrefix = "darwin-x86-64",
        cinteropName = "osx",
    ),
    MacOSArm64(
        rustTriple = "aarch64-apple-darwin",
        jnaResourcePrefix = "darwin-aarch64",
        cinteropName = "osx",
    ),
    LinuxX64(
        rustTriple = "x86_64-unknown-linux-gnu",
        jnaResourcePrefix = "linux-x86-64",
        cinteropName = "linux",
    ),
    LinuxArm64(
        rustTriple = "aarch64-unknown-linux-gnu",
        jnaResourcePrefix = "linux-aarch64",
        cinteropName = "linux",
    );

    override val friendlyName = name

    override val supportedKotlinPlatformTypes = arrayOf(KotlinPlatformType.jvm, KotlinPlatformType.native)
    override fun outputFileName(crateName: String, crateType: CrateType): String? {
        return when {
            isWindows() -> crateType.outputFileNameForMinGW(crateName)
            isMacOS() -> crateType.outputFileNameForMacOS(crateName)
            isLinux() -> crateType.outputFileNameForLinux(crateName)
            else -> null
        }
    }

    override fun toString() = rustTriple

    fun isWindows() = windowsTargets.contains(this)

    fun isMacOS() = macOSTargets.contains(this)

    fun isLinux() = linuxTargets.contains(this)

    companion object {
        val windowsTargets = arrayOf(MinGWX64)
        val macOSTargets = arrayOf(MacOSX64, MacOSArm64)
        val linuxTargets = arrayOf(LinuxX64, LinuxArm64)
    }
}
