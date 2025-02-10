/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.tasks

import io.gitlab.trixnity.gradle.tasks.CommandTask
import io.gitlab.trixnity.gradle.utils.CommandSpec
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File

abstract class RustUpTask : CommandTask() {
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val rustUp: Property<File>

    internal fun rustUp(
        vararg argument: String,
        action: CommandSpec.() -> Unit = {},
    ) = rustUp.map { it as Any }.orElse("rustup").flatMap { rustUp ->
        if (rustUp is File) {
            command(rustUp) {
                arguments(*argument)
                action()
            }
        } else {
            command("rustup") {
                arguments(*argument)
                action()
            }
        }
    }
}
