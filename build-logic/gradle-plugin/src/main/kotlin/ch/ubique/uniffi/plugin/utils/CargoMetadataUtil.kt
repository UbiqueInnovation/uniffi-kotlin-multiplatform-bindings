package ch.ubique.uniffi.plugin.utils

import ch.ubique.uniffi.plugin.model.CargoMetadata

val CargoMetadata.targetPackage: CargoMetadata.Package
    get() = requireNotNull(packages.find { it.id == resolvedDependency.root }) {
        "Couldn't find the package corresponding to ${resolvedDependency.root}!"
    }
