/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.uniffi.gradle.tasks

import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*

@CacheableTask
abstract class BuildBindingsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val crateDirectory: DirectoryProperty

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
    @get:Input
    @get:Optional
    abstract val libraryName: Property<String>

    /**
     * When `--library` is passed, only generate bindings for one crate.
     * When `--library` is not passed, use this as the crate name instead of attempting to
     * locate and parse Cargo.toml.
     */
    @get:Input
    @get:Optional
    abstract val crateName: Property<String>

    /**
     * Path to the UDL file, or cdylib if `library-mode` is specified
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val source: RegularFileProperty

    @TaskAction
    fun buildBindings(): Unit = with(project) {
        exec { spec ->
            spec.workingDir(crateDirectory)
            spec.commandLine(
                bindgen.get()
            )
            if (outputDirectory.isPresent) {
                spec.args("--out-dir", outputDirectory.get())
            }
            if (config.isPresent) {
                spec.args("--config", config.get())
            }
            if (libraryFile.isPresent) {
                spec.args("--lib-file", libraryFile.get())
            }
            if (libraryMode.get()) {
                spec.args("--library")
            }
            if (crateName.isPresent) {
                spec.args("--crate", crateName.get())
            }
            spec.args(source.get())
        }.assertNormalExitValue()

        val defFilePath = outputDirectory.get().file("nativeInterop/cinterop/${libraryName.get()}.def")
        val defFileFile = defFilePath.asFile
        defFileFile.parentFile.mkdirs()
        defFileFile.writeText("staticLibraries = lib${libraryName.get()}.a\n")
    }
}
