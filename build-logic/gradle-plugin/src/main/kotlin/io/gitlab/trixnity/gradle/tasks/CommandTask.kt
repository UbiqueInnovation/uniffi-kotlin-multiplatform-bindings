/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.tasks

import io.gitlab.trixnity.gradle.utils.Command
import io.gitlab.trixnity.gradle.utils.CommandResult
import io.gitlab.trixnity.gradle.utils.CommandSpec
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.of
import java.io.File
import javax.inject.Inject

abstract class CommandTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val additionalEnvironment: MapProperty<String, Any>

    @get:Input
    @get:Optional
    abstract val additionalEnvironmentPath: ListProperty<File>

    @get:Inject
    internal abstract val projectLayout: ProjectLayout

    @get:Inject
    internal abstract val providerFactory: ProviderFactory

    internal open fun configureFromProperties(spec: CommandSpec) = with(spec) {
        for ((key, value) in additionalEnvironment.get()) {
            additionalEnvironment(key, value)
        }
        additionalEnvironmentPath(additionalEnvironmentPath)
    }

    @JvmName("commandWithRegularFile")
    internal fun command(
        command: Provider<RegularFile>,
        action: CommandSpec.() -> Unit = {},
    ): Provider<CommandResult> = command(command.map { it.asFile.name }) {
        additionalEnvironmentPath(command.map { it.asFile.parentFile })
        action()
    }

    internal fun command(
        command: String,
        action: CommandSpec.() -> Unit = {},
    ) = command(providerFactory.provider { command }, action)

    internal fun command(
        command: Provider<String>,
        action: CommandSpec.() -> Unit = {},
    ): Provider<CommandResult> = providerFactory.of(Command::class) {
        it.parameters.command.set(command)
        CommandSpec(projectLayout, providerFactory, it.parameters).apply {
            configureFromProperties(this)
            action()
        }
    }
}
