/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package net.folivo.uniffi.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property

@CacheableTask
abstract class InstallBindgenTask : DefaultTask() {

    @get:Input
    val quiet: Property<Boolean> = project.objects.property<Boolean>().convention(true)

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bindgenCratePath: DirectoryProperty

    @get:OutputDirectory
    abstract val installDirectory: DirectoryProperty

    @get:OutputFile
    @Suppress("LeakingThis")
    val bindgen: RegularFileProperty = project.objects.fileProperty()
        .convention(installDirectory.file("bin/${BuildConfig.BINDGEN_BIN}"))

    @TaskAction
    fun buildBindings(): Unit = with(project) {
        exec { spec ->
            spec.commandLine(
                "cargo",
                "install",
                "--root",
                installDirectory.get().asFile.canonicalPath,
                "--bin",
                BuildConfig.BINDGEN_BIN,
            )

            if (quiet.get()) {
                spec.args("--quiet")
            }

            if (bindgenCratePath.isPresent) {
                spec.args("--path", bindgenCratePath.get())
            } else {
                spec.args("${BuildConfig.BINDGEN_CRATE}@${BuildConfig.BINDGEN_VERSION}")
            }
        }.assertNormalExitValue()
    }
}
