/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.uniffi.tasks

import io.gitlab.trixnity.gradle.cargo.tasks.CargoPackageTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*

@CacheableTask
abstract class BuildBindingsTask : CargoPackageTask() {
    /**
     * Directory in which to write generated files. Default is same folder as .udl file.
     */
    @get:OutputDirectory
    @get:Optional
    abstract val outputDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bindgen: RegularFileProperty

    /**
     * Path to the optional uniffi config file.
     * If not provided, uniffi-bindgen will try to guess it from the UDL's file location.
     */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val config: RegularFileProperty

    /**
     * Extract proc-macro metadata from a native lib (cdylib or staticlib) for this crate.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val libraryFile: RegularFileProperty

    /**
     * Pass in a cdylib path rather than a UDL file
     */
    @get:Input
    @get:Optional
    abstract val libraryMode: Property<Boolean>

    /**
     * The library name, as defined in Cargo.toml.
     */
    @Suppress("LeakingThis")
    @get:Input
    val libraryCrateName: Provider<String> = cargoPackage.map { it.libraryCrateName }

    /**
     * When `--library` is passed, only generate bindings for one crate.
     * When `--library` is not passed, use this as the crate name instead of attempting to
     * locate and parse Cargo.toml.
     */
    @Suppress("LeakingThis")
    @get:Input
    @get:Optional
    val crateName: Provider<String> = cargoPackage.map { it.libraryCrateName }

    /**
     * Path to the UDL file, or cdylib if `library-mode` is specified
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val source: RegularFileProperty

    @TaskAction
    fun buildBindings() {
        command(bindgen) {
            workingDirectory(root)
            if (outputDirectory.isPresent) {
                arguments("--out-dir", outputDirectory.get())
            }
            if (config.isPresent) {
                arguments("--config", config.get())
            }
            if (libraryFile.isPresent) {
                arguments("--lib-file", libraryFile.get())
            }
            if (libraryMode.get()) {
                arguments("--library")
            }
            if (crateName.isPresent) {
                arguments("--crate", crateName.get())
            }
            arguments(source.get())
            suppressXcodeIosToolchains()
        }.get().assertNormalExitValue()

        val defFilePath = outputDirectory.get().file("nativeInterop/cinterop/${libraryCrateName.get()}.def")
        val defFileFile = defFilePath.asFile
        defFileFile.parentFile.mkdirs()
        defFileFile.writeText("staticLibraries = lib${libraryCrateName.get()}.a\n")
    }
}
