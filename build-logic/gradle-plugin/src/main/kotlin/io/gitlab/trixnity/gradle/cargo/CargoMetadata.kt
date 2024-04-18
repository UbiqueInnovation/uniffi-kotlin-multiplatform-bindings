/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo

import io.gitlab.trixnity.gradle.cargo.rust.CrateType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class CargoMetadata(
    @SerialName("packages") val packages: List<Package>,
    @SerialName("resolve") val resolvedDependency: ResolvedDependency,
    @SerialName("target_directory") val targetDirectory: String,
    @SerialName("workspace_root") val workspaceRoot: String,
) {
    @Serializable
    data class Package(
        @SerialName("name") val name: String,
        @SerialName("id") val id: String,
        @SerialName("manifest_path") val manifestPath: String,
        @SerialName("dependencies") val dependencies: List<Dependency>,
        @SerialName("targets") val targets: List<Target>,
        @SerialName("features") val features: Map<String, List<String>>,
    ) {
        @Serializable
        data class Dependency(
            @SerialName("name") val name: String,
            @SerialName("kind") val kind: String?,
            @SerialName("optional") val optional: Boolean,
            @SerialName("uses_default_features") val usesDefaultFeatures: Boolean,
            @SerialName("features") val features: List<String>,
        )

        @Serializable
        data class Target(
            @SerialName("kind") val kinds: List<CargoTargetKind>,
            @SerialName("crate_types") val crateTypes: List<CrateType>,
            @SerialName("name") val name: String,
            @SerialName("src_path") val sourcePath: String,
        )
    }

    @Serializable
    data class ResolvedDependency(
        @SerialName("nodes") val nodes: List<Node>,
        @SerialName("root") val root: String?
    ) {
        @Serializable
        data class Node(
            @SerialName("id") val id: String,
            @SerialName("dependencies") val dependencies: List<String>,
            @SerialName("features") val features: List<String>,
        )
    }

    companion object {
        fun fromJsonString(s: String): CargoMetadata = json.decodeFromString(s)
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }
}
