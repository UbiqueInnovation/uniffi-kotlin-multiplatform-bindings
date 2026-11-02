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
}
