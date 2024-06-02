/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.tasks

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal interface GloballyLockedTask : Task {
    @get:Internal
    val jvmLocalLockHolder: Property</* workaround for Gradle's ClassLoader isolation*/ Any>

    @get:Internal
    val rootProjectDirectory: DirectoryProperty
}

internal fun Project.useGlobalLock() {
    val rootProjectDirectory = rootProject.layout.projectDirectory
    val jvmLocalLockHolderProvider =
        gradle.sharedServices.registerIfAbsent("jvmLocalLock", JvmLocalLockHolder::class) {}
    tasks.withType<GloballyLockedTask>().configureEach { task ->
        task.jvmLocalLockHolder.set(jvmLocalLockHolderProvider)
        task.rootProjectDirectory.set(rootProjectDirectory)
        task.usesService(jvmLocalLockHolderProvider)
    }
}

internal abstract class JvmLocalLockHolder : BuildService<BuildServiceParameters.None> {
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    val jvmLocalLockMap = ConcurrentHashMap<String, ReentrantLock>()
}

/**
 * Execute the given [action] while blocking other actions with the same [identifier]. Based on a [FileLock] combined
 * with a [ReentrantLock].
 */
internal fun GloballyLockedTask.globalLock(identifier: String, action: () -> Unit) {
    // TODO: Replace calls to this function using a proper Gradle API, as well as CargoBuildTask which uses a file lock

    // On Gradle, tasks in different projects are loaded with different ClassLoader. Therefore, the GloballyLockedTask
    // instance set by CargoPlugin in a project cannot be cast to GloballyLockedTask in another project.
    // See https://github.com/gradle/gradle/issues/17559 here for more details.

    val globalLockService = jvmLocalLockHolder.get()
    val getJvmLocalLockMap = globalLockService::class.java.getMethod("getJvmLocalLockMap")

    @Suppress("UNCHECKED_CAST")
    val jvmLocalLockMap = getJvmLocalLockMap.invoke(globalLockService) as ConcurrentHashMap<String, ReentrantLock>

    GlobalLock(jvmLocalLockMap, rootProjectDirectory.get().asFile, identifier).use(action)
}

/**
 * A lock using a temporary file in `<root>/.gradle/rust`.
 * @param root The path to the root project.
 * @param identifier The name of the file.
 */
private class GlobalLock(
    jvmLocalLockMap: ConcurrentHashMap<String, ReentrantLock>,
    private val root: File,
    private val identifier: String,
) {
    private val path = root.resolve(".gradle/rust/$identifier.lock").absoluteFile.apply {
        parentFile.mkdirs()
    }
    private val jvmLocalLock = jvmLocalLockMap.getOrPut(path.path, ::ReentrantLock)
    private val file = RandomAccessFile(path, "rw")
    private var channel: FileChannel? = null
    private var fileLock: FileLock? = null

    @OptIn(ExperimentalContracts::class)
    fun use(action: () -> Unit) {
        contract {
            callsInPlace(action, InvocationKind.EXACTLY_ONCE)
        }
        try {
            lock()
            action()
        } finally {
            unlock()
        }
    }

    private fun lock() {
        jvmLocalLock.lock()
        val newChannel = file.channel
        channel = newChannel
        fileLock = newChannel.lock()
    }

    private fun unlock() {
        fileLock?.close()
        fileLock = null
        channel?.close()
        channel = null
        jvmLocalLock.unlock()
    }
}
