package ch.ubique.uniffi.plugin

import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.utils.BindgenSource

internal object Constants {
    object Plugins {
        const val KMP_PLUGIN = "org.jetbrains.kotlin.multiplatform"
        const val ANDROID_PLUGIN = "com.android.kotlin.multiplatform.library"
    }


    val BINDGEN_SOURCE: BindgenSource = BindgenSource.Git(
        repository = "https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings.git",
        bindgenName = BINDGEN_BIN_NAME,
        packageName = BINDGEN_PACKAGE_NAME,
    )

    const val BINDGEN_BIN_NAME = "uniffi-bindgen-kotlin-multiplatform"
    const val BINDGEN_PACKAGE_NAME = "uniffi_bindgen_kotlin_multiplatform"

    const val RUNTIME_VERSION = PluginVersions.RUNTIME_VERSION
    const val JNA_VERSION = "5.17.0"
    const val ATOMICFU_VERSION = "0.32.1"
    const val OKIO_VERSION = "3.9.1"
    const val COROUTINES_VERSION = "1.9.0"
    const val DATETIME_VERSION = "0.7.1"
}

internal object Tasks {
    const val INSTALL_BINDGEN = "installBindgen"
    const val BUILD_LIB_FOR_BINDINGS = "buildLibraryForBindings"
    const val BUILD_BINDINGS = "buildBindings"
    const val GENERATE_DUMMY_DEF = "generateDummyDefFile"

    const val MERGE_ANDROID_JNI_LIBS = "mergeUniffiAndroidJniLibs"
    const val MERGE_ANDROID_TEST_RESOURCES = "mergeUniffiAndroidHostTestResources"

    fun cargoBuild(
        target: BuildTarget.RustTarget,
        release: Boolean
    ): String = "cargoBuild${target.name}${Strings.release(release)}"

    fun generateDefFile(
        buildTarget: BuildTarget
    ): String = "generateDefFileFor${buildTarget.name}"

    fun copyNativeLibraries(
        rustTarget: BuildTarget.RustTarget,
        buildTarget: BuildTarget,
        release: Boolean,
        dynamic: Boolean,
    ): String =
        "copyNative${Strings.dynamic(dynamic)}Libs${rustTarget.name}${Strings.release(release)}For${buildTarget.name}"

    fun copyNativeLibraries(
        buildTarget: BuildTarget,
        release: Boolean,
        dynamic: Boolean,
    ): String =
        "copyNative${Strings.dynamic(dynamic)}Libs${Strings.release(release)}${buildTarget.name}"
}

internal object Strings {
    fun release(release: Boolean) = if (release) {
        "Release"
    } else {
        "Debug"
    }

    fun dynamic(dynamic: Boolean) = if (dynamic) {
        "Dynamic"
    } else {
        "Static"
    }
}
