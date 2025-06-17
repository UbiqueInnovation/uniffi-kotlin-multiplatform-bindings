package ch.ubique.uniffi.plugin.extensions

import ch.ubique.uniffi.plugin.model.BuildTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import java.io.File

fun KotlinNativeCompilation.useRustUpLinker() {
    val rustUpHome = getRustUpHome()
    val toolchain = getActiveToolchain()

    val currentTarget = BuildTarget.RustTarget.forCurrentPlatform

    val rustUpLinker = File(rustUpHome)
        .resolve("toolchains/$toolchain/lib/rustlib")
        .resolve(currentTarget.rustTriple)
        .resolve("bin/gcc-ld/ld.lld")

    kotlinOptions.freeCompilerArgs += listOf(
        "-Xoverride-konan-properties=linker.${currentTarget.konanName}=${rustUpLinker.absolutePath}"
    )
}

private fun getRustUpHome(): String {
    val process = ProcessBuilder("rustup", "show", "home")
        .start()

    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()

    check(exitCode == 0) {
        println(output)
        "Failed to get rustup home with exit code $exitCode"
    }

    return output.trim()
}

private fun getActiveToolchain(): String {
    val process = ProcessBuilder("rustup", "show", "active-toolchain")
        .start()

    val output = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()

    check(exitCode == 0) {
        println(output)
        "Failed to get rustup active toolchain with exit code $exitCode"
    }

    val activeToolchains = output.trim().split("\n")
    val toolchain = activeToolchains.firstNotNullOf {
        it.trim().split(" ").getOrNull(0)?.takeUnless(String::isEmpty)
    }

    return toolchain
}
