package ch.ubique.uniffi.plugin.utils

import ch.ubique.uniffi.plugin.model.RustHost
import java.io.File

object NdkUtil {
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

    private fun File.selectLatestVersion(): File? = listFiles()?.maxWithOrNull { file0, file1 ->
        val version0 = file0.extractVersion()
        val version1 = file1.extractVersion()
        version0.compareTo(version1)
    }

    private fun ndkRootFromSdkRoot(sdkRoot: File, ndkVersion: String? = null): File? {
        return sdkRoot.resolve("ndk").run {
            ndkVersion?.let(::resolve)?.takeIf(File::exists) ?: selectLatestVersion()
        }
    }

    private fun ndkRootFromAndroidNdkRoot(): File? =
        System.getenv("ANDROID_NDK_ROOT")?.let(::File)

    private fun tryRetrieveNdkRoot(
        sdkRoot: File,
        ndkVersion: String? = null,
        ndkRoot: File? = null,
    ): File? = ndkRoot ?: ndkRootFromSdkRoot(sdkRoot, ndkVersion) ?: ndkRootFromAndroidNdkRoot()

    private fun ndkToolchainDir(
        sdkRoot: File,
        ndkVersion: String? = null,
        ndkRoot: File? = null,
    ) = tryRetrieveNdkRoot(sdkRoot, ndkVersion, ndkRoot)?.run {
        resolve("toolchains/llvm/prebuilt").resolve(RustHost.current.platform.ndkHostTag)
    }

    fun ndkEnvVariables(
        sdkRoot: File,
        apiLevel: Int = 21,
        ndkVersion: String? = null,
        ndkRoot: File? = null,
        rustTriple: String,
        ndkLlvmTriple: String,
    ): Map<String, String> {
        val actualNdkRoot = tryRetrieveNdkRoot(
            sdkRoot,
            ndkVersion,
            ndkRoot
        )!!
        val toolchainBinaryDir = ndkToolchainDir(
            sdkRoot,
            ndkVersion,
            ndkRoot
        )!!.resolve("bin")
        val currentPlatform = RustHost.current.platform
        val clang =
            toolchainBinaryDir.resolve(currentPlatform.convertExeName("${ndkLlvmTriple}$apiLevel-clang"))
        val clangCpp =
            toolchainBinaryDir.resolve(currentPlatform.convertExeName("${ndkLlvmTriple}$apiLevel-clang++"))
        val ar = toolchainBinaryDir.resolve(currentPlatform.convertExeName("llvm-ar"))
        val ranlib = toolchainBinaryDir.resolve(currentPlatform.convertExeName("llvm-ranlib"))
        val escapedRustTriple = rustTriple.replace('-', '_')
        return mapOf(
            "ANDROID_HOME" to sdkRoot.path,
            "ANDROID_NDK_HOME" to actualNdkRoot.path,
            "ANDROID_NDK_ROOT" to actualNdkRoot.path,
            "CARGO_TARGET_${escapedRustTriple.uppercase()}_LINKER" to clang.path,
            "CC_$escapedRustTriple" to clang.path,
            "CXX_$escapedRustTriple" to clangCpp.path,
            "AR_$escapedRustTriple" to ar.path,
            "RANLIB_$escapedRustTriple" to ranlib.path,
        )
    }
}