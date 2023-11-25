/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package net.folivo.uniffi.gradle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.file.Directory
import java.io.ByteArrayOutputStream

@Serializable
data class CargoMetadata(
    @SerialName("target_directory")
    val targetDirectory: String,
)

fun Project.getCargoMetadata(crateDirectory: Directory): CargoMetadata {
    val outputStream = ByteArrayOutputStream()

    exec { spec ->
        spec.workingDir(crateDirectory)
        spec.commandLine("cargo", "metadata", "--format-version", "1")
        spec.standardOutput = outputStream
    }.assertNormalExitValue()

    return json.decodeFromString(outputStream.toString())
}

private val json = Json {
    ignoreUnknownKeys = true
}
