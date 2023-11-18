package net.folivo.uniffi.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

interface UniFfiExtension {
    /**
     * The crate directory.
     */
    val crateDirectory: DirectoryProperty

    /**
     * The crate name.
     */
    val crateName: Property<String>

    /**
     * The crate name. Defaults to `"${crateName}"`.
     */
    val libraryName: Property<String>

    /**
     * The UDL file. Defaults to `"${crateDirectory}/src/${crateName}.udl"`.
     */
    val udlFile: RegularFileProperty

    /**
     * The UDL namespace. Defaults to `"${crateName}"`.
     */
    val namespace: Property<String>

    /**
     * The crate build profile. Defaults to `"debug"`.
     */
    val profile: Property<String>

    /**
     * The bindgen crate path.
     * This is only necessary if you want to build with a local bindgen source crate.
     */
    val bindgenCratePath: DirectoryProperty
}
