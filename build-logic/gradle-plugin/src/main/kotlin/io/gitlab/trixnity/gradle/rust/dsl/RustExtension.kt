/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.rust.dsl

import io.gitlab.trixnity.gradle.RustHost
import io.gitlab.trixnity.gradle.cargo.dsl.CargoExtension
import io.gitlab.trixnity.gradle.utils.PluginUtils
import io.gitlab.trixnity.gradle.utils.command
import io.gitlab.trixnity.uniffi.gradle.PluginIds
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.property
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.io.File

abstract class RustExtension(project: Project) {
    /**
     * The directory where `cargo` and `rustup` are installed. Defaults to `~/.cargo/bin`.
     */
    val toolchainDirectory: Property<File> =
        project.objects.property<File>()
            .convention(RustHost.current.platform.defaultToolchainDirectory)
}

fun KotlinMultiplatformExtension.hostNativeTarget(
    configure: KotlinNativeTarget.() -> Unit = {},
): KotlinNativeTarget {
    return when (RustHost.Platform.current) {
        RustHost.Platform.Windows -> mingwX64(configure)
        RustHost.Platform.MacOS -> when (RustHost.Arch.current) {
            RustHost.Arch.X64 -> macosX64(configure)
            RustHost.Arch.Arm64 -> macosArm64(configure)
        }

        RustHost.Platform.Linux -> when (RustHost.Arch.current) {
            RustHost.Arch.X64 -> linuxX64(configure)
            RustHost.Arch.Arm64 -> linuxArm64(configure)
        }
    }
}

fun KotlinMultiplatformExtension.hostNativeTarget(
    name: String,
    configure: KotlinNativeTarget.() -> Unit = {},
): KotlinNativeTarget {
    return when (RustHost.Platform.current) {
        RustHost.Platform.Windows -> mingwX64(name, configure)
        RustHost.Platform.MacOS -> when (RustHost.Arch.current) {
            RustHost.Arch.X64 -> macosX64(name, configure)
            RustHost.Arch.Arm64 -> macosArm64(name, configure)
        }

        RustHost.Platform.Linux -> when (RustHost.Arch.current) {
            RustHost.Arch.X64 -> linuxX64(name, configure)
            RustHost.Arch.Arm64 -> linuxArm64(name, configure)
        }
    }
}

fun KotlinNativeCompilation.useRustUpLinker() {
    compilerOptions.configure {
        PluginUtils.ensurePluginIsApplied(
            project,
            PluginUtils.PluginInfo(
                "Rust Kotlin Multiplatform",
                PluginIds.RUST_KOTLIN_MULTIPLATFORM,
            ),
            PluginUtils.PluginInfo(
                "Cargo Kotlin Multiplatform",
                PluginIds.CARGO_KOTLIN_MULTIPLATFORM,
            ),
        )
        val toolchainDirectory =
            project.extensions.findByType<CargoExtension>()?.toolchainDirectory
                ?: project.extensions.findByType<RustExtension>()?.toolchainDirectory
        val rustUpLinker = project.rustUpLinker(toolchainDirectory!!.get()).absolutePath
        freeCompilerArgs.add(
            "-Xoverride-konan-properties=linker.${RustHost.current.konanName}-${target.konanTarget.name}=$rustUpLinker"
        )
    }
}

private fun Project.rustUpLinker(toolchainDirectory: File): File {
    val rustUpHome = command("rustup") {
        arguments("show", "home")
        additionalEnvironmentPath(toolchainDirectory)
        captureStandardOutput()
    }.get().apply {
        assertNormalExitValue()
    }.standardOutput!!.trim()

    val activeToolchains = command("rustup") {
        arguments("show", "active-toolchain")
        additionalEnvironmentPath(toolchainDirectory)
        captureStandardOutput()
    }.get().apply {
        assertNormalExitValue()
    }.standardOutput!!.trim().split('\n')

    val toolchain = activeToolchains.firstNotNullOf {
        it.trim().split(' ').getOrNull(0)?.takeUnless(String::isEmpty)
    }
    // TODO: check whether the following works well on Windows
    return File(rustUpHome).resolve("toolchains/$toolchain/lib/rustlib")
        .resolve(RustHost.current.rustTarget.rustTriple).resolve("bin/gcc-ld/ld.lld")
}
