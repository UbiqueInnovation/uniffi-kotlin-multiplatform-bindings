/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.tasks

import io.gitlab.trixnity.gradle.CargoHost
import io.gitlab.trixnity.gradle.tasks.CommandTask
import io.gitlab.trixnity.gradle.utils.CommandSpec
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

@Suppress("LeakingThis")
abstract class CargoTask : CommandTask() {
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val cargo: Property<File>

    init {
        if (cargo.isPresent) {
            additionalEnvironmentPath.add(cargo.map { it.parentFile })
        }
        additionalEnvironmentPath.add(CargoHost.Platform.current.defaultCargoInstallationDir)
    }

    internal fun cargo(
        vararg argument: String,
        action: CommandSpec.() -> Unit = {},
    ) = command("cargo") {
        arguments(*argument)
        action()
    }
}
