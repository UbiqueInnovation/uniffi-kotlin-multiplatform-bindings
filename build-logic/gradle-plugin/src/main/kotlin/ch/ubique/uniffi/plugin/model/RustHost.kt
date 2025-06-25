package ch.ubique.uniffi.plugin.model

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

data class RustHost(val platform: Platform, val arch: Arch) {
    enum class Platform {
        MacOS, Linux, Windows;

        val isCurrent: Boolean
            get() = when (this) {
                MacOS -> defaultOperatingSystem.isMacOsX
                Linux -> defaultOperatingSystem.isLinux
                Windows -> defaultOperatingSystem.isWindows
            }

        val ndkHostTag: String
            get() = when (this) {
                Windows -> "windows-x86_64"
                MacOS -> "darwin-x86_64"
                Linux -> "linux-x86_64"
            }

        fun convertExeName(name: String): String = when (this) {
            Windows -> "$name.exe"
            else -> name
        }

        companion object {
            private val defaultOperatingSystem = DefaultNativePlatform.getCurrentOperatingSystem()

            val current: Platform = entries.firstOrNull { it.isCurrent }
                ?: throw IllegalStateException("Unsupported os: ${defaultOperatingSystem.displayName}")
        }
    }

    enum class Arch {
        X64, Arm64;

        val isCurrent: Boolean
            get() = when (this) {
                X64 -> defaultArchitecture.isAmd64
                Arm64 -> defaultArchitecture.isArm64
            }

        companion object {
            private val defaultArchitecture = DefaultNativePlatform.getCurrentArchitecture()

            val current: Arch = entries.firstOrNull { it.isCurrent }
                ?: throw IllegalStateException("Unsupported arch: ${defaultArchitecture.displayName}")
        }
    }

    val rustTarget: String
        get() = when (platform) {
            Platform.MacOS -> when (arch) {
                Arch.X64 -> "x86_64-apple-darwin"
                Arch.Arm64 -> "aarch64-apple-darwin"
            }
            Platform.Linux -> when (arch) {
                Arch.X64 -> "x86_64-unknown-linux-gnu"
                Arch.Arm64 -> "aarch64-unknown-linux-gnu"
            }
            Platform.Windows -> when (arch) {
                Arch.X64 -> "x86_64-pc-windows-msvc"
                Arch.Arm64 -> "aarch64-pc-windows-msvc"
            }
        }

    companion object {
        val current: RustHost = RustHost(Platform.current, Arch.current)
    }
}