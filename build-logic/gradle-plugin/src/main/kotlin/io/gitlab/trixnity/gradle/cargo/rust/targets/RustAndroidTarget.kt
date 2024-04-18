/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.rust.targets

import io.gitlab.trixnity.gradle.CargoHost
import io.gitlab.trixnity.gradle.cargo.rust.CrateType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.io.File
import java.io.Serializable

/**
 * Represents a Rust Android target for a single ABI supported by NDK.
 */
enum class RustAndroidTarget(
    override val rustTriple: String,
    // `RustAndroidTarget` should not implement `RustJvmTarget` because Android .so files are handled and copied
    // differently than the dynamic libraries of normal desktop JVM targets.
    //
    // override val jnaResourcePrefix: String,
    /**
     * The name of the ABI, which is used in APK files to distinguish libraries of different CPU architectures.
     */
    val androidAbiName: String,
    /**
     * The LLVM triple prefix of the ABI, which is used by the LLVM toolchain in NDK.
     */
    val ndkLlvmTriple: String = rustTriple,
    /**
     * The LLVM triple of the ABI, which is used in the NDK library directory.
     */
    val ndkLibraryTriple: String = rustTriple,
    // TODO: add `RustAndroidNativeTarget` that implements `RustNativeTarget` for Android NDK Kotlin/Native targets
) : RustMobileTarget, /* RustJvmTarget, */ Serializable {
    Arm64(
        rustTriple = "aarch64-linux-android",
        // jnaResourcePrefix = "android-aarch64",
        androidAbiName = "arm64-v8a",
    ),
    ArmV7(
        rustTriple = "armv7-linux-androideabi",
        // jnaResourcePrefix = "android-arm",
        androidAbiName = "armeabi-v7a",
        ndkLlvmTriple = "armv7a-linux-androideabi",
        ndkLibraryTriple = "arm-linux-androideabi",
    ),
    X64(
        rustTriple = "x86_64-linux-android",
        // jnaResourcePrefix = "android-x86-64",
        androidAbiName = "x86_64",
    ),
    X86(
        rustTriple = "i686-linux-android",
        // jnaResourcePrefix = "android-x86",
        androidAbiName = "x86",
    );

    override val friendlyName = "Android$name"

    override val supportedKotlinPlatformTypes = arrayOf(KotlinPlatformType.androidJvm)
    override fun outputFileName(crateName: String, crateType: CrateType): String? =
        crateType.outputFileNameForLinux(crateName)

    override fun toString() = rustTriple

    fun ndkEnvVariables(
        sdkRoot: File,
        apiLevel: Int = 21,
        ndkVersion: String? = null,
        ndkRoot: File? = null,
    ): Map<String, Any> {
        val actualNdkRoot = tryRetrieveNdkRoot(sdkRoot, ndkVersion, ndkRoot)!!
        val toolchainBinaryDir = ndkToolchainDir(sdkRoot, ndkVersion, ndkRoot)!!.resolve("bin")
        return mapOf(
            "ANDROID_HOME" to sdkRoot,
            "ANDROID_NDK_HOME" to actualNdkRoot,
            "ANDROID_NDK_ROOT" to actualNdkRoot,
            "CARGO_TARGET_${
                rustTriple.replace('-', '_').uppercase()
            }_LINKER" to toolchainBinaryDir.resolve("${ndkLlvmTriple}$apiLevel-clang"),
            "CC_$rustTriple" to toolchainBinaryDir.resolve("${ndkLlvmTriple}$apiLevel-clang"),
            "CXX_$rustTriple" to toolchainBinaryDir.resolve("${ndkLlvmTriple}$apiLevel-clang++"),
            "AR_$rustTriple" to toolchainBinaryDir.resolve("llvm-ar"),
            "RANLIB_$rustTriple" to toolchainBinaryDir.resolve("llvm-ranlib"),
            "CFLAGS_$rustTriple" to "-D__ANDROID_MIN_SDK_VERSION__=$apiLevel",
            "CXXFLAGS_$rustTriple" to "-D__ANDROID_MIN_SDK_VERSION__=$apiLevel",
        )
    }

    fun ndkLibraryDirectories(
        sdkRoot: File,
        apiLevel: Int = 21,
        ndkVersion: String? = null,
        ndkRoot: File? = null,
    ): List<File> {
        val toolchainLibraryDir = ndkToolchainDir(sdkRoot, ndkVersion, ndkRoot)!!
            .resolve("sysroot/usr/lib")
            .resolve(ndkLibraryTriple)
        return listOf(
            toolchainLibraryDir,
            toolchainLibraryDir.resolve(apiLevel.toString())
        )
    }

    companion object {
        private fun ndkRootFromSdkRoot(sdkRoot: File, ndkVersion: String? = null): File? {
            return sdkRoot.resolve("ndk").run {
                ndkVersion?.let(::resolve)?.takeIf(File::exists) ?: selectLatestVersion()
            }
        }

        private fun ndkRootFromAndroidNdkRoot(): File? = System.getenv("ANDROID_NDK_ROOT")?.let(::File)

        private fun tryRetrieveNdkRoot(
            sdkRoot: File,
            ndkVersion: String? = null,
            ndkRoot: File? = null,
        ): File? = ndkRoot ?: ndkRootFromSdkRoot(sdkRoot, ndkVersion) ?: ndkRootFromAndroidNdkRoot()

        private val CargoHost.Platform.ndkHostTag: String
            get() = when (this) {
                CargoHost.Platform.Windows -> "windows-x86_64"
                CargoHost.Platform.MacOS -> "darwin-x86_64"
                CargoHost.Platform.Linux -> "linux-x86_64"
            }

        private fun ndkToolchainDir(
            sdkRoot: File,
            ndkVersion: String? = null,
            ndkRoot: File? = null,
        ) = tryRetrieveNdkRoot(sdkRoot, ndkVersion, ndkRoot)?.run {
            resolve("toolchains/llvm/prebuilt").resolve(CargoHost.current.platform.ndkHostTag)
        }
    }
}

fun RustAndroidTarget(androidAbiName: String): RustAndroidTarget =
    RustAndroidTarget.entries.first { it.androidAbiName == androidAbiName }

private fun File.selectLatestVersion(): File? = listFiles()?.maxWithOrNull { file0, file1 ->
    val version0 = file0.extractVersion()
    val version1 = file1.extractVersion()
    version0.compareTo(version1)
}

private fun File.extractVersion(): List<Int> = name.split('.').mapNotNull { it.toIntOrNull() }

private fun <T : Comparable<T>> Iterable<T>.compareTo(other: Iterable<T>): Int {
    val thisIterator = iterator()
    val otherIterator = other.iterator()

    while (thisIterator.hasNext() && otherIterator.hasNext()) {
        val thisElem = thisIterator.next()
        val otherElem = otherIterator.next()

        val comparison = thisElem.compareTo(otherElem)
        if (comparison != 0) {
            return comparison
        }
    }

    return when {
        thisIterator.hasNext() -> 1
        otherIterator.hasNext() -> -1
        else -> 0
    }
}
