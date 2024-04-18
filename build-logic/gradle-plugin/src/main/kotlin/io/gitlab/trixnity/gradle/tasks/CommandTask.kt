/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.tasks

import io.gitlab.trixnity.gradle.utils.Command
import io.gitlab.trixnity.gradle.utils.command
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File

abstract class CommandTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val additionalEnvironment: MapProperty<String, Any>

    @get:Input
    @get:Optional
    abstract val additionalEnvironmentPath: ListProperty<File>

    internal open fun configureFromProperties(command: Command) = with(command) {
        for ((key, value) in additionalEnvironment.get()) {
            additionalEnvironment(key, value)
        }
        additionalEnvironmentPath(additionalEnvironmentPath)
    }

    internal fun command(command: Provider<RegularFile>): Command {
        return project
            .command(command)
            .apply { configureFromProperties(this) }
    }

    internal fun command(command: String): Command {
        return project
            .command(command)
            .apply { configureFromProperties(this) }
    }
}
