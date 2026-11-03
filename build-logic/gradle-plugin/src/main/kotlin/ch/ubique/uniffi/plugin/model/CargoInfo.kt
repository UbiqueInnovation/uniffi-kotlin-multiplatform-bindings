package ch.ubique.uniffi.plugin.model

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

internal data class CargoInfo(
    val packageName: Provider<String>,
    val libraryName: Provider<String>,
    /** The crate types of the library target, i.e. its `[lib] crate-type`. */
    val declaredCrateTypes: Provider<List<CrateType>>,
    /** The workspace members declaring a `cdylib` or `staticlib`. */
    val workspaceLibraryPackages: Provider<List<String>>,
    val targetDirectory: Provider<Directory>,
)