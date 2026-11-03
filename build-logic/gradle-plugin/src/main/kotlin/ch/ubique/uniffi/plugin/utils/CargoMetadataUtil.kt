package ch.ubique.uniffi.plugin.utils

import ch.ubique.uniffi.plugin.model.CargoMetadata
import ch.ubique.uniffi.plugin.model.CrateType
import java.io.File

fun CargoMetadata.targetPackage(packageDirectory: File): CargoMetadata.Package {
    val manifest = packageDirectory.resolve("Cargo.toml").canonicalFile
    return requireNotNull(packages.find { File(it.manifestPath).canonicalFile == manifest }) {
        "Couldn't find the package corresponding to $manifest!"
    }
}

/** The workspace members whose library target declares a `cdylib` or `staticlib`. */
fun CargoMetadata.workspaceForeignLibraryPackages(): List<String> = packages
    .filter { it.id in workspaceMembers }
    .filter { pkg ->
        pkg.targets.firstOrNull()?.crateTypes.orEmpty()
            .any { it == CrateType.SystemDynamicLibrary || it == CrateType.SystemStaticLibrary }
    }
    .map { it.name }
    .sorted()
