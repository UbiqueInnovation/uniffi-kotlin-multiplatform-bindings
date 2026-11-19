package ch.ubique.uniffi.plugin.model

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

internal data class CargoInfo(
    /** The output of `cargo metadata` for the package. */
    val metadataJson: Provider<String>,
    val packageName: Provider<String>,
    val libraryName: Provider<String>,
    /** The crate types of the library target, i.e. its `[lib] crate-type`. */
    val declaredCrateTypes: Provider<List<CrateType>>,
    /** The package's dependencies by their name in its `Cargo.toml`. */
    val dependencyNames: Provider<Set<String>>,
    /** The workspace crates the package depends on that declare a `cdylib`. */
    val cdylibDependencies: Provider<Set<String>>,
    val workspaceRoot: Provider<Directory>,
    val targetDirectory: Provider<Directory>,
)
