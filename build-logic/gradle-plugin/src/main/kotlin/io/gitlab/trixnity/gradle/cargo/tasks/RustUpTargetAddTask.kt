/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.tasks

import io.gitlab.trixnity.gradle.cargo.rust.targets.RustTarget
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.util.concurrent.locks.ReentrantLock

@CacheableTask
abstract class RustUpTargetAddTask : RustUpTask() {
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
        // TODO: Rewrite the following using a proper Gradle API, as well as CargoBuildTask which uses a file lock
        targetAddLock.lock()
        try {
            if (!isToolchainInstalled()) {
                rustUp("target", "add", rustTarget.get().rustTriple)
                    .get().apply {
                        assertNormalExitValue()
                    }
            }
        } finally {
            targetAddLock.unlock()
        }
    }

    companion object {
        private val targetAddLock = ReentrantLock()
    }
}
