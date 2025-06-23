package ch.ubique.uniffi.plugin.extensions

import ch.ubique.uniffi.plugin.model.BuildTarget
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import java.io.File
import kotlin.text.trim

fun KotlinNativeCompilation.useRustUpLinker() {
    val rustUpHome = getRustUpHome(project)
    val toolchain = getActiveToolchain(project)

    val currentTarget = BuildTarget.RustTarget.forCurrentPlatform

    val rustUpLinker = File(rustUpHome)
        .resolve("toolchains/$toolchain/lib/rustlib")
        .resolve(currentTarget.rustTriple)
        .resolve("bin/gcc-ld/ld.lld")

    for (target in BuildTarget.fromTargetName(target.name)?.targets ?: listOf()) {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xoverride-konan-properties=linker.${currentTarget.konanName}-${target.konanName}=${rustUpLinker.absolutePath}"
        )
    }
}

private fun getRustUpHome(project: Project): String {
    return project.providers.exec {
        commandLine("rustup", "show", "home")
    }.standardOutput.asText.get().trim()
}

private fun getActiveToolchain(project: Project): String {
    val output = project.providers.exec {
        commandLine("rustup", "show", "active-toolchain")
    }.standardOutput.asText.get().trim()

    val activeToolchains = output.trim().split("\n")
    val toolchain = activeToolchains.firstNotNullOf {
        it.trim().split(" ").getOrNull(0)?.takeUnless(String::isEmpty)
    }

    return toolchain
}
