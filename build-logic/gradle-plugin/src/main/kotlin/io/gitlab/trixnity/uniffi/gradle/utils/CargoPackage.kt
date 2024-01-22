/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.gradle.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.file.Directory
import org.tomlj.Toml
import java.io.File
import java.lang.IllegalStateException

/**
 * Represents a Cargo package.
 */
class CargoPackage(private val root: File) {
    constructor(root: Directory) : this(root.asFile)

    private val metadata = CargoMetadata.fromJsonString(run {
        val process = ProcessBuilder("cargo", "metadata", "--format-version", "1").directory(root).start()
        val metadataJsonString = process.inputReader().readText()

        process.waitFor()
        if (process.exitValue() != 0) {
            throw IllegalStateException("Cargo metadata command failed: ${process.errorReader().readText()}")
        }

        metadataJsonString
    })

    /**
     * The manifest file of the package.
     */
    private val manifestFile = root.resolve("Cargo.toml")

    /**
     * The parsed manifest from Cargo.toml in the package directory.
     */
    private val manifest = Toml.parse(manifestFile.reader())

    /**
     * The name of the package, as defined in Cargo.toml.
     */
    val name = manifest.getString("package.name")!!

    /**
     * The name of the library crate of this package, as defined in Cargo.toml. This defaults to the package name
     * replaced with underscores if omitted.
     */
    val libraryName = manifest.getString("lib.name") ?: name.replace('-', '_')

    /**
     * The crate types of the library crate of this package, as defined in Cargo.toml.
     */
    val crateTypes = run outer@{
        val crateTypes = manifest.getArray("lib.crate-type") ?: run {
            val isProcMacro = manifest.getBoolean("lib.proc-macro") ?: false
            return@outer setOf(
                if (isProcMacro) "proc-macro" else "lib"
            )
        }
        crateTypes.toList().mapNotNull { it as? String }.toSet()
    }

    /**
     * The directory where the build result is stored by the profile and the target triple.
     */
    fun outputDirectory(profile: String = "debug", target: String? = null): File {
        val targetDirectory = File(metadata.workspaceRoot).resolve("target")
        val targetSpecificDirectory = if (target != null) targetDirectory.resolve(target) else targetDirectory
        return targetSpecificDirectory.resolve(profile)
    }
}

@Serializable
private data class CargoMetadata(
    @SerialName("target_directory") val targetDirectory: String,
    @SerialName("workspace_root") val workspaceRoot: String,
) {
    companion object {
        fun fromJsonString(s: String): CargoMetadata = json.decodeFromString(s)
        val json = Json {
            ignoreUnknownKeys = true
        }
    }
}
