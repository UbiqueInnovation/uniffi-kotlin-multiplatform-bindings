/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.uniffi.tasks

import io.gitlab.trixnity.gradle.cargo.tasks.CargoTask
import io.gitlab.trixnity.uniffi.gradle.BuildConfig
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property

@CacheableTask
abstract class InstallBindgenTask : CargoTask() {
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
    fun buildBindings() {
        cargo("install") {
            arguments("--root", installDirectory)
            arguments("--bin", BuildConfig.BINDGEN_BIN)
            if (quiet.get()) {
                arguments("--quiet")
            }
            if (bindgenCratePath.isPresent) {
                arguments("--path", bindgenCratePath.get())
            } else {
                arguments("${BuildConfig.BINDGEN_CRATE}@${BuildConfig.BINDGEN_VERSION}")
            }
            suppressXcodeIosToolchains()
        }.get().assertNormalExitValue()
    }
}
