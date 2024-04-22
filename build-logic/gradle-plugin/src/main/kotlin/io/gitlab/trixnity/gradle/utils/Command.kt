/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.utils

import io.gitlab.trixnity.gradle.CargoHost
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.process.ExecSpec
import org.gradle.process.internal.ExecException
import java.io.ByteArrayOutputStream
import java.io.File

internal fun Project.command(command: Provider<String>) = Command(project, command)

internal fun Project.command(command: String) = project.command(project.providers.provider { command })

@JvmName("commandFromFileProvider")
internal fun Project.command(command: Provider<File>) = project.command(command.map { it.name }).apply {
    additionalEnvironmentPath(command.map { it.parentFile })
}

@JvmName("commandFromRegularFileProvider")
internal fun Project.command(command: Provider<RegularFile>) = project.command(command.map { it.asFile })

/**
 * Utility class for running a command
 */
internal class Command(
    private val project: Project,
    private val command: Provider<String>,
) {
    private val arguments = project.objects.listProperty<Any>()
    private var workingDirectory = project.objects.directoryProperty().convention(project.layout.projectDirectory)
    private val additionalEnvironment = project.objects.mapProperty<String, Any>().apply {
        put("PATH", PathList(project))
    }

    private var suppressXcodeIosToolchains: Boolean = false

    fun arguments(vararg argument: Any) = argument.forEach {
        if (it is Provider<*>) {
            arguments.add(it)
        } else {
            arguments.add(it)
        }
    }

    fun workingDirectory(dir: Provider<Directory>) = workingDirectory.set(dir)

    fun workingDirectory(dir: Directory) = workingDirectory.set(dir)

    fun additionalEnvironmentPath(vararg paths: Any) {
        for (path in paths) {
            additionalEnvironment("PATH", path)
        }
    }

    fun additionalEnvironmentPath(vararg paths: Provider<File>) {
        for (path in paths) {
            additionalEnvironment("PATH", path)
        }
    }

    fun additionalEnvironment(key: String, value: Any) {
        if (value is Provider<*>) additionalEnvironment(key, value)
        else additionalEnvironment(key, project.providers.provider { value })
    }

    fun <T : Any> additionalEnvironment(key: String, value: Provider<T>) {
        val oldEnvironment = additionalEnvironment.getting(key)
        if (oldEnvironment.isPresent) {
            val oldEnvironmentValue = oldEnvironment.get()
            if (oldEnvironmentValue is PathList) {
                additionalEnvironment.put(key, oldEnvironmentValue + value)
                return
            }
        }

        additionalEnvironment.put(key, value)
    }

    /**
     * @see suppressXcodeIosToolchains checks the value of PATH and moves all Xcode toolchain paths to back before running
     * the command.
     *
     * Xcode prepends its toolchain paths to PATH. If Gradle is invoked by Xcode to build an iOS framework, iOS
     * toolchain paths will be prepended to PATH. This hides executables like `/usr/bin/cc` or libraries like
     * `libc++` or `libSystem`, which was to be used to link libraries for the host system, resulting in link error
     * during running tasks like binding generation.
     */
    fun suppressXcodeIosToolchains() {
        suppressXcodeIosToolchains = true
    }

    fun run(
        captureStandardOutput: Boolean = false,
        captureStandardError: Boolean = false,
        action: ExecSpec.() -> Unit = {},
    ): CommandResult {
        val environment = additionalEnvironment.get().toMutableMap().apply {
            // Append the PATH of the current Java process
            val newPath = this["PATH"] as PathList + project.environmentPath + project.packageManagerInstallDirectories
            if (suppressXcodeIosToolchains && CargoHost.Platform.MacOS.isCurrent) {
                this["PATH"] = newPath.suppressPathsUnder(File("/Applications/Xcode.app"))
            } else {
                this["PATH"] = newPath
            }
        }

        val command = command.resolveAbsolutePath(environment["PATH"] as PathList).get()
        val arguments = arguments.get().map { it.toString() }
        val workingDirectory = workingDirectory.get()

        val standardOutputStream = if (captureStandardOutput) ByteArrayOutputStream() else null
        val standardErrorStream = if (captureStandardError) ByteArrayOutputStream() else null

        project.logger.info("command ${listOf(command) + arguments} is running...")
        project.logger.info("cwd: $workingDirectory")
        project.logger.info("environment: $environment")

        val result = project.exec { exec ->
            exec.setIgnoreExitValue(true)
            exec.commandLine(command)
            exec.args(arguments)
            exec.workingDir(workingDirectory)
            if (additionalEnvironment.isPresent) {
                exec.environment.putAll(environment)
            }
            standardOutputStream?.let { exec.standardOutput = it }
            standardErrorStream?.let { exec.errorOutput = it }
            exec.action()
        }

        return CommandResult(
            command,
            arguments,
            workingDirectory,
            standardOutputStream?.use { it.toString() },
            standardErrorStream?.use { it.toString() },
            result.exitValue,
        )
    }

    /**
     * Try to resolve the canonical path of the given command, using the given list of paths.
     */
    private fun Provider<String>.resolveAbsolutePath(
        additionalPaths: PathList
    ): Provider<String> = zip((additionalPaths + project.environmentPath).paths) { command, paths ->
        for (path in paths) {
            path.resolve(command).run {
                if (exists()) return@zip absolutePath
            }
            path.resolve(CargoHost.Platform.current.convertExeName(command)).run {
                if (exists()) return@zip absolutePath
            }
        }
        return@zip command
    }
}

internal data class CommandResult(
    val command: String,
    val arguments: List<String>,
    val workingDirectory: Directory,
    val standardOutput: String?,
    val standardError: String?,
    val statusCode: Int,
) {
    fun assertNormalExitValue() {
        if (statusCode != 0) {
            val message = StringBuilder().apply {
                append("command ${listOf(command) + arguments} failed with code $statusCode:\n")
                append("cwd: $workingDirectory\n")
                val standardOutput = standardOutput?.trim(Char::isWhitespace)?.takeIf(String::isNotEmpty)
                val standardError = standardError?.trim(Char::isWhitespace)?.takeIf(String::isNotEmpty)
                if (standardOutput != null) {
                    if (standardError != null) {
                        append("stdout:\n")
                    }
                    append(standardOutput)
                }
                if (standardError != null) {
                    if (standardOutput != null) {
                        append("\nstderr:\n")
                    }
                    append(standardError)
                }
            }
            throw ExecException(message.toString())
        }
    }
}

/**
 * Utility class for storing a list of paths to be joined to a string with separators.
 */
private class PathList(
    private val project: Project,
    val paths: Provider<List<File>>,
) {
    constructor(
        project: Project,
        paths: List<File> = emptyList(),
    ) : this(project, project.providers.provider { paths })

    constructor(
        project: Project,
        arguments: String,
    ) : this(project, arguments.split(CargoHost.Platform.current.pathSeparator).filter(String::isNotEmpty).map(::File))

    operator fun plus(other: PathList): PathList {
        if (project != other.project) throw GradleException("project of two PathLists must be the same")
        return PathList(project, paths.zip(other.paths) { l, r -> l + r })
    }

    operator fun <T : Any> plus(arg: Provider<T>) = PathList(project, paths.zip(arg) { l, r ->
        when (r) {
            is File -> l + r
            is Iterable<*> -> l + r.map { if (it is File) it else File(it.toString()) }
            else -> l + File(r.toString())
        }
    })

    operator fun plus(arg: File) = this + project.providers.provider { arg }

    fun suppressPathsUnder(root: File): PathList = PathList(project, paths.map { paths ->
        val pathsToSuppress = paths.filter { it.startsWith(root) }
        val otherPaths = paths.filterNot { it.startsWith(root) }
        otherPaths + pathsToSuppress
    })

    /**
     * Joins the arguments into one string.
     */
    fun joinToString(): Provider<String> = paths.map { it.joinToString(separator) { file -> file.absolutePath } }

    override fun toString(): String = joinToString().get()

    companion object {
        val separator: String = CargoHost.Platform.current.pathSeparator
    }
}

private val Project.environmentPath
    get() = PathList(
        this, System.getenv("PATH")!!
    )

private val Project.packageManagerInstallDirectories
    get() = PathList(
        this, CargoHost.current.packageManagerInstallDirectory.map(::File)
    )
