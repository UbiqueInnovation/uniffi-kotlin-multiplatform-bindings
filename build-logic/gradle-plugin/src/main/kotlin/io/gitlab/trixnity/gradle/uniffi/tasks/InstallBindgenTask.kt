/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.uniffi.tasks

import io.gitlab.trixnity.gradle.cargo.tasks.CargoTask
import io.gitlab.trixnity.gradle.uniffi.dsl.BindgenSource
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

    @get:Input
    abstract val bindgenSource: Property<BindgenSource>

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
            when (val source = bindgenSource.get()) {
                is BindgenSource.Registry -> {
                    arguments("${source.packageName}@${source.version}")
                    if (source.registry != null) {
                        arguments("--registry", source.registry)
                    }
                }

                is BindgenSource.Path -> arguments("--path", source.path)
                is BindgenSource.Git -> {
                    arguments("--git", source.repository)
                    when (source.commit) {
                        is BindgenSource.Git.Commit.Branch -> arguments("--branch", source.commit.branch)
                        is BindgenSource.Git.Commit.Tag -> arguments("--tag", source.commit.tag)
                        is BindgenSource.Git.Commit.Revision -> arguments("--rev", source.commit.revision)
                        else -> {}
                    }
                }
            }
            suppressXcodeIosToolchains()
        }.get().assertNormalExitValue()
    }
}
