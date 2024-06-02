/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.tasks

import io.gitlab.trixnity.gradle.cargo.rust.targets.RustTarget
import io.gitlab.trixnity.gradle.tasks.GloballyLockedTask
import io.gitlab.trixnity.gradle.tasks.globalLock
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class RustUpTargetAddTask : RustUpTask(), GloballyLockedTask {
    @get:Input
    abstract val rustTarget: Property<RustTarget>

    init {
        outputs.upToDateWhen {
            it as RustUpTargetAddTask
            it.isToolchainInstalled()
        }
    }

    private fun isToolchainInstalled(): Boolean {
        val installedTargets = rustUp("target", "list") {
            captureStandardOutput()
        }.get().run {
            standardOutput!!
                .split('\n')
                .filter { it.endsWith(" (installed)") }
                .map { it.substringBefore(" (installed)") }
                .toSet()
        }

        return installedTargets.contains(rustTarget.get().rustTriple)
    }

    @TaskAction
    fun installToolchain() {
        globalLock("rustUpTargetAdd") {
            if (!isToolchainInstalled()) {
                rustUp("target", "add", rustTarget.get().rustTriple)
                    .get().apply {
                        assertNormalExitValue()
                    }
            }
        }
    }
}
