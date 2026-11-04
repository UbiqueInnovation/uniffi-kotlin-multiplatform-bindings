package ch.ubique.uniffi.plugin.utils

import ch.ubique.uniffi.plugin.model.CargoMetadata
import kotlin.test.Test
import kotlin.test.assertEquals

class CargoMetadataUtilTest {
    @Test
    fun `workspace foreign library packages are the members declaring a cdylib or staticlib`() {
        fun pkg(name: String, kind: String, crateTypes: String) = """
            {"name": "$name", "id": "$name-id", "manifest_path": "/ws/$name/Cargo.toml",
             "dependencies": [], "features": {},
             "targets": [{"kind": [$kind], "crate_types": [$crateTypes], "name": "$name", "src_path": "/ws/$name/src/lib.rs"}]}
        """
        val metadata = CargoMetadata.fromJsonString("""
            {"target_directory": "/ws/target", "workspace_root": "/ws",
             "workspace_members": ["wallet-id", "util-id", "helper-id", "cli-id", "ios-id"],
             "packages": [
                ${pkg("wallet", "\"cdylib\", \"staticlib\", \"lib\"", "\"cdylib\", \"staticlib\", \"lib\"")},
                ${pkg("util", "\"cdylib\", \"lib\"", "\"cdylib\", \"lib\"")},
                ${pkg("helper", "\"lib\"", "\"lib\"")},
                ${pkg("cli", "\"bin\"", "\"bin\"")},
                ${pkg("ios", "\"staticlib\"", "\"staticlib\"")},
                ${pkg("external", "\"cdylib\"", "\"cdylib\"")}
             ]}
        """)

        assertEquals(listOf("ios", "util", "wallet"), metadata.workspaceForeignLibraryPackages())
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
