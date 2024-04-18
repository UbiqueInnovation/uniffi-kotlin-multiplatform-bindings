/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.rustlink

import io.gitlab.trixnity.gradle.CargoHost
import io.gitlab.trixnity.gradle.utils.command
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File

/**
 * This plugin is for exposing the following functions.
 */
class RustLinkPlugin : Plugin<Project> {
    override fun apply(target: Project) = Unit
}

// TODO: properly expose the following using DSL objects

fun KotlinMultiplatformExtension.hostNativeTarget(
    configure: KotlinNativeTarget.() -> Unit = {},
): KotlinNativeTarget {
    return when (CargoHost.Platform.current) {
        CargoHost.Platform.Windows -> mingwX64(configure)
        CargoHost.Platform.MacOS -> when (CargoHost.Arch.current) {
            CargoHost.Arch.X64 -> macosX64(configure)
            CargoHost.Arch.Arm64 -> macosArm64(configure)
        }

        CargoHost.Platform.Linux -> when (CargoHost.Arch.current) {
            CargoHost.Arch.X64 -> linuxX64(configure)
            CargoHost.Arch.Arm64 -> linuxArm64(configure)
        }
    }
}

fun KotlinMultiplatformExtension.hostNativeTarget(
    name: String,
    configure: KotlinNativeTarget.() -> Unit = {},
): KotlinNativeTarget {
    return when (CargoHost.Platform.current) {
        CargoHost.Platform.Windows -> mingwX64(name, configure)
        CargoHost.Platform.MacOS -> when (CargoHost.Arch.current) {
            CargoHost.Arch.X64 -> macosX64(name, configure)
            CargoHost.Arch.Arm64 -> macosArm64(name, configure)
        }

        CargoHost.Platform.Linux -> when (CargoHost.Arch.current) {
            CargoHost.Arch.X64 -> linuxX64(name, configure)
            CargoHost.Arch.Arm64 -> linuxArm64(name, configure)
        }
    }
}

fun KotlinNativeCompilation.useRustUpLinker() {
    compilerOptions.configure {
        freeCompilerArgs.add(
            "-Xoverride-konan-properties=linker.${CargoHost.current.konanName}-${target.konanTarget.name}=${project.rustUpLinker().canonicalPath}"
        )
    }
}

private fun Project.rustUpLinker(): File {
    val rustUpHome = command("rustup").apply {
        arguments("show", "home")
        additionalEnvironmentPath(CargoHost.Platform.current.defaultCargoInstallationDir)
    }.run(captureStandardOutput = true).standardOutput!!.trim()

    val activeToolchains = command("rustup").apply {
        arguments("show", "active-toolchain")
        additionalEnvironmentPath(CargoHost.Platform.current.defaultCargoInstallationDir)
    }.run(captureStandardOutput = true).standardOutput!!.trim().split('\n')

    val toolchain = activeToolchains.firstNotNullOf {
        it.trim().split(' ').getOrNull(0)?.takeUnless(String::isEmpty)
    }
    // TODO: check whether the following works well on Windows
    return File(rustUpHome).resolve("toolchains/$toolchain/lib/rustlib")
        .resolve(CargoHost.current.hostTarget.rustTriple).resolve("bin/gcc-ld/ld.lld")
}
