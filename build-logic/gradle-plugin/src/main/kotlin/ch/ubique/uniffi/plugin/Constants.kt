package ch.ubique.uniffi.plugin

import ch.ubique.uniffi.plugin.utils.BindgenSource

object Constants {
    val BINDGEN_SOURCE: BindgenSource = BindgenSource.Git(
        repository = "https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings.git",
        bindgenName = BINDGEN_BIN_NAME,
        packageName = BINDGEN_PACKAGE_NAME,
    )

    const val BINDGEN_BIN_NAME = "uniffi-bindgen-kotlin-multiplatform"
    const val BINDGEN_PACKAGE_NAME = "uniffi_bindgen_kotlin_multiplatform"

    const val RUNTIME_VERSION = PluginVersions.RUNTIME_VERSION
    const val JNA_VERSION = "5.17.0"
    const val ATOMICFU_VERSION = "0.25.0"
    const val OKIO_VERSION = "3.9.1"
    const val COROUTINES_VERSION = "1.9.0"
    const val DATETIME_VERSION = "0.6.0"
}