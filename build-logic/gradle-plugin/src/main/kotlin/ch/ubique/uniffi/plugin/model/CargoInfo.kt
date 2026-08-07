package ch.ubique.uniffi.plugin.model

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

internal data class CargoInfo(
    val packageName: Provider<String>,
    val libraryName: Provider<String>,
    val targetDirectory: Provider<Directory>,
)