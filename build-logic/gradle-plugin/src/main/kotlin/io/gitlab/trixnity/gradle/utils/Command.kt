/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.utils

import io.gitlab.trixnity.gradle.CargoHost
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.logging.Logging
import org.gradle.api.provider.*
import org.gradle.process.ExecOperations
import org.gradle.process.internal.ExecException
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

internal fun Project.command(
    command: Provider<String>,
    action: CommandSpec.() -> Unit = {},
) = providers.of(Command::class.java) {
    it.parameters.command.set(command)
    CommandSpec(project.layout, project.providers, it.parameters).action()
}

internal fun Project.command(
    command: String,
    action: CommandSpec.() -> Unit = {},
) = project.command(project.providers.provider { command }, action)

internal interface CommandParameters : ValueSourceParameters {
    val command: Property<String>
    val arguments: ListProperty<Any>
    val workingDirectory: DirectoryProperty
    val additionalEnvironment: MapProperty<String, Any>
    val suppressXcodeIosToolchains: Property<Boolean>
    val captureStandardOutput: Property<Boolean>
    val captureStandardError: Property<Boolean>
}

internal class CommandSpec(
    projectLayout: ProjectLayout,
    private val providerFactory: ProviderFactory,
    private val parameters: CommandParameters,
) {
    // Workaround to prevent StackOverflow in additionalEnvironment
    private val lastAdditionalEnvironment = mutableMapOf<String, Provider<out Any>>().apply {
        this["PATH"] = providerFactory.provider { PathList() }
    }

    init {
        parameters.workingDirectory.convention(projectLayout.projectDirectory)
        updateAdditionalEnvironment("PATH")
        parameters.suppressXcodeIosToolchains.convention(false)
        parameters.captureStandardOutput.convention(false)
        parameters.captureStandardError.convention(false)
    }

    private fun updateAdditionalEnvironment(key: String) {
        parameters.additionalEnvironment.put(key, lastAdditionalEnvironment[key]!!)
    }

    fun arguments(vararg argument: Any) = argument.forEach {
        if (it is Provider<*>) {
            parameters.arguments.add(it)
        } else {
            parameters.arguments.add(it)
        }
    }

    fun workingDirectory(dir: Provider<Directory>) = parameters.workingDirectory.set(dir)

    fun workingDirectory(dir: Directory) = parameters.workingDirectory.set(dir)

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
        else additionalEnvironment(key, providerFactory.provider { value })
    }

    fun <T : Any> additionalEnvironment(key: String, provider: Provider<T>) {
        val lastProvider = lastAdditionalEnvironment[key]
        if (lastProvider == null) {
            lastAdditionalEnvironment[key] = provider
        } else {
            lastAdditionalEnvironment[key] = lastProvider.zip(provider) { oldValue, value ->
                if (oldValue is PathList) {
                    val newPathList: PathList = when (value) {
                        is Iterable<*> -> oldValue + value.map { File(it.toString()) }
                        is File -> oldValue + value
                        else -> oldValue + value.toString()
                    }
                    newPathList
                } else {
                    value
                }
            }
        }
        updateAdditionalEnvironment(key)
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
        parameters.suppressXcodeIosToolchains.set(true)
    }

    fun captureStandardOutput() {
        parameters.captureStandardOutput.set(true)
    }

    fun captureStandardError() {
        parameters.captureStandardError.set(true)
    }
}

/**
 * Utility class for running a command
 */
internal abstract class Command @Inject internal constructor(
    private val execOperations: ExecOperations,
) : ValueSource<CommandResult, CommandParameters> {
    override fun obtain(): CommandResult {
        val environment = parameters.additionalEnvironment.get().toMutableMap().apply {
            val oldPath = this["PATH"] as PathList
            // Append the PATH of the current Java process
            val newPath = oldPath + environmentPath + packageManagerInstallDirectories
            if (parameters.suppressXcodeIosToolchains.get() && CargoHost.Platform.MacOS.isCurrent) {
                this["PATH"] = newPath.suppressPathsUnder(File("/Applications/Xcode.app"))
            } else {
                this["PATH"] = newPath
            }
        }

        val command = parameters.command.resolveAbsolutePath(environment["PATH"] as PathList).get()
        val arguments = parameters.arguments.get().map { it.toString() }
        val workingDirectory = parameters.workingDirectory.get()

        val standardOutputStream = if (parameters.captureStandardOutput.get()) ByteArrayOutputStream() else null
        val standardErrorStream = if (parameters.captureStandardError.get()) ByteArrayOutputStream() else null

        Logging.getLogger("Command").apply {
            info("command ${listOf(command) + arguments} is running...")
            info("cwd: $workingDirectory")
            info("environment: $environment")
        }

        val result = execOperations.exec { exec ->
            exec.setIgnoreExitValue(true)
            exec.commandLine(command)
            exec.args(arguments)
            exec.workingDir(workingDirectory)
            exec.environment.putAll(environment)
            standardOutputStream?.let { exec.standardOutput = it }
            standardErrorStream?.let { exec.errorOutput = it }
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
    ): Provider<String> = map { command ->
        for (path in (additionalPaths + environmentPath).paths) {
            path.resolve(command).run {
                if (exists()) return@map absolutePath
            }
            path.resolve(CargoHost.Platform.current.convertExeName(command)).run {
                if (exists()) return@map absolutePath
            }
        }
        command
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

private val environmentPath = PathList(System.getenv("PATH")!!)

private val packageManagerInstallDirectories = PathList(CargoHost.current.packageManagerInstallDirectories.map(::File))
