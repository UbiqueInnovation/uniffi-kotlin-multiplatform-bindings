package ch.ubique.uniffi.plugin.utils

import ch.ubique.uniffi.plugin.model.CargoMetadata
import java.io.File

fun CargoMetadata.targetPackage(packageDirectory: File): CargoMetadata.Package {
    val manifest = packageDirectory.resolve("Cargo.toml").canonicalFile
    return requireNotNull(packages.find { File(it.manifestPath).canonicalFile == manifest }) {
        "Couldn't find the package corresponding to $manifest!"
    }
}
