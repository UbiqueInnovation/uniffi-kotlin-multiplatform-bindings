package dev.oxikt.gradle.plugin.utils

import dev.oxikt.gradle.plugin.model.CargoMetadata
import kotlin.test.Test
import kotlin.test.assertEquals

class CargoMetadataUtilTest {
    @Test
    fun `cdylib workspace dependencies are found through other members`() {
        fun dependency(name: String, kind: String? = null) = """
            {"name": "$name", "path": "/ws/$name", "kind": ${kind?.let { "\"$it\"" }},
             "optional": false, "uses_default_features": true, "features": []}
        """
        fun pkg(name: String, crateTypes: String, vararg dependencies: String) = """
            {"name": "$name", "id": "$name-id", "manifest_path": "/ws/$name/Cargo.toml",
             "dependencies": [${dependencies.joinToString(",")}], "features": {},
             "targets": [
                {"kind": ["custom-build"], "crate_types": ["bin"], "name": "build-script-build", "src_path": "/ws/$name/build.rs"},
                {"kind": [$crateTypes], "crate_types": [$crateTypes], "name": "$name", "src_path": "/ws/$name/src/lib.rs"}
             ]}
        """
        val metadata = CargoMetadata.fromJsonString("""
            {"target_directory": "/ws/target", "workspace_root": "/ws",
             "workspace_members": ["wallet-id", "crypto-id", "util-id", "plain-id", "tests-id", "builder-id"],
             "packages": [
                ${pkg("wallet", "\"cdylib\", \"lib\"", dependency("crypto"), dependency("plain"), dependency("tests", "dev"), dependency("builder", "build"))},
                ${pkg("crypto", "\"lib\"", dependency("util"))},
                ${pkg("util", "\"cdylib\", \"staticlib\", \"lib\"")},
                ${pkg("plain", "\"lib\", \"staticlib\"")},
                ${pkg("tests", "\"cdylib\", \"lib\"")},
                ${pkg("builder", "\"cdylib\", \"lib\"")}
             ]}
        """)
        fun cdylibs(name: String) = metadata.cdylibWorkspaceDependencies(metadata.packages.single { it.name == name })

        // util through crypto. Dev and build dependencies don't count, and neither does the
        // package's own cdylib or a dependency declaring only a staticlib.
        assertEquals(setOf("util"), cdylibs("wallet"))
        assertEquals(setOf("util"), cdylibs("crypto"))
        assertEquals(emptySet(), cdylibs("util"))
    }

    @Test
    fun `dependency names are the names in the manifest`() {
        fun dependency(name: String, rename: String?) = """
            {"name": "$name", ${rename?.let { "\"rename\": \"$it\"," }.orEmpty()}
             "kind": null, "optional": false, "uses_default_features": true, "features": []}
        """
        val metadata = CargoMetadata.fromJsonString("""
            {"target_directory": "/ws/target", "workspace_root": "/ws",
             "packages": [{"name": "wallet", "id": "wallet-id", "manifest_path": "/ws/wallet/Cargo.toml",
                           "features": {}, "targets": [],
                           "dependencies": [${dependency("serde", null)}, ${dependency("util", "wallet-util")}]}]}
        """)

        assertEquals(setOf("serde", "wallet-util"), metadata.packages.single().dependencyNames())
    }
}
