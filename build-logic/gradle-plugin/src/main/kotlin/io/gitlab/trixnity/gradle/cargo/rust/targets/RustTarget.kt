/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.rust.targets

import io.gitlab.trixnity.gradle.cargo.rust.CrateType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 *
 * Represents a Rust target.
 */
sealed interface RustTarget {
    /**
     * The target triple string passed via --target argument to Cargo commands to build for this target.
     */
    val rustTriple: String

    /**
     * The Kotlin platform types where the outputs of this target can be used.
     */
    val supportedKotlinPlatformTypes: Array<KotlinPlatformType>

    /**
     * The name used to be displayed to users or to be used in task names. Must be unique over all targets like
     * `rustTriple`.
     */
    val friendlyName: String

    /**
     * The name of the output file by the name of the crate and the type.
     */
    fun outputFileName(crateName: String, crateType: CrateType): String?

    companion object {
        val entries: Array<RustTarget> = arrayOf(
            RustAndroidTarget.entries,
            RustAppleMobileTarget.entries,
            RustPosixTarget.entries,
            RustWindowsTarget.entries,
        ).flatMap { it }.toTypedArray()
    }
}

fun RustTarget(konanTarget: KonanTarget): RustTarget = when (konanTarget) {
    // Android NDK targets are not supported
    // iOS ARM32 is not supported
    KonanTarget.IOS_ARM64 -> RustAppleMobileTarget.IosArm64
    KonanTarget.IOS_SIMULATOR_ARM64 -> RustAppleMobileTarget.IosSimulatorArm64
    KonanTarget.IOS_X64 -> RustAppleMobileTarget.IosX64
    // Linux ARM32 is not supported
    KonanTarget.LINUX_ARM64 -> RustPosixTarget.LinuxArm64
    // Linux MIPS targets are not supported
    KonanTarget.LINUX_X64 -> RustPosixTarget.LinuxX64
    KonanTarget.MACOS_ARM64 -> RustPosixTarget.MacOSArm64
    KonanTarget.MACOS_X64 -> RustPosixTarget.MacOSX64
    KonanTarget.MINGW_X64 -> RustPosixTarget.MinGWX64
    // MinGW x86 is not supported
    // TODO: add tvOS targets
    // WASM targets are not supported
    // TODO: add watchOS targets
    else -> throw IllegalArgumentException("KonanTarget $konanTarget is not supported")
}

fun RustTarget(rustTriple: String): RustTarget = RustTarget.entries.first { it.rustTriple == rustTriple }
