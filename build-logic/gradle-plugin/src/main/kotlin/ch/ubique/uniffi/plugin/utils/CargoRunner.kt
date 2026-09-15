package ch.ubique.uniffi.plugin.utils

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.process.ExecOperations
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.StandardCharsets

class CargoRunner(
    private val execOperations: ExecOperations,
    private val logger: Logger,
    private val useCross: Boolean = false,
    action: CargoRunner.() -> Unit = {},
) {
    private val arguments: MutableList<String> = mutableListOf()

    private var redirectErrorStream: Boolean = false

    private val environment: MutableMap<String, String> = mutableMapOf()

    private var workingDir: File? = null

    init {
        action(this)
    }

    fun argument(arg: String) {
        arguments.add(arg)
    }

    fun redirectErrorStream(redirect: Boolean) {
        redirectErrorStream = redirect
    }

    fun env(key: String, value: String) {
        environment.put(key, value)
    }

    fun workdir(dir: File) {
        workingDir = dir
    }

    fun run(): String {
        val commandName = if (useCross) "cross" else "cargo"
        val command = RustLocator.findRustExecutable(commandName).path
        val result = execute(command, arguments)

        // Check if maybe just a target is missing and install it using rustup.
        val targetToInstall = result.stderr.lineSequence()
            .mapNotNull {
                Regex("""consider downloading the target with `rustup target add ([^`]+)`""")
                    .find(it)?.groupValues?.get(1)
            }
            .firstOrNull()

        if (result.exitValue != 0 && targetToInstall != null) {
            logger.warn(
                "Failed to run '$command ${arguments.joinToString(" ")}'. " +
                    "Trying to install the Rust target with 'rustup target add $targetToInstall'",
            )
            installRustTarget(targetToInstall)

            // If the rustup command succeeded, retry the failed command.
            return run()
        }

        if (result.exitValue != 0) {
            val commandLine = "$command ${arguments.joinToString(" ")}".trim()
            val details = buildString {
                if (result.stdout.isNotBlank()) appendLine("stdout:\n${result.stdout}")
                if (result.stderr.isNotBlank()) appendLine("stderr:\n${result.stderr}")
            }.trim()
            throw GradleException(
                buildString {
                    append("Failed to run '$commandLine' with exit code ${result.exitValue}")
                    if (details.isNotEmpty()) appendLine("\n$details")
                }
            )
        }

        return if (redirectErrorStream) {
            result.stdout + "\n" + result.stderr
        } else {
            result.stdout
        }
    }

    private fun installRustTarget(target: String) {
        val rustup = RustLocator.findRustExecutable("rustup").path
        val lockDirectory = File(
            System.getProperty("java.io.tmpdir"),
            "ch.ubique.uniffi-${System.getProperty("user.name").replace(Regex("[^A-Za-z0-9._-]"), "_")}",
        ).also { it.mkdirs() }
        val lockFile = lockDirectory.resolve("rustup.lock")

        val result = withGlobalFileLock(lockFile) {
            execute(
                command = rustup,
                arguments = listOf("target", "add", target),
            )
        }

        if (result.exitValue != 0) {
            val output = (result.stdout + "\n" + result.stderr).trim()
            throw GradleException(
                buildString {
                    append("Failed to run 'rustup target add $target' with exit code ${result.exitValue}")
                    if (output.isNotEmpty()) appendLine("\n$output")
                }
            )
        }
    }

    private fun execute(
        command: String,
        arguments: List<String>,
    ): CommandResult {
        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val processEnvironment = environment.toMutableMap().apply {
            putIfAbsent("GIT_TERMINAL_PROMPT", "0")
        }

        logger.lifecycle(
            buildString {
                append("Running ")
                append(formatCommand(command, arguments))
                workingDir?.let { append(" (working directory: ${it.path})") }
            },
        )

        val result = execOperations.exec { spec ->
            spec.commandLine(command, *arguments.toTypedArray())
            spec.isIgnoreExitValue = true
            spec.standardInput = ByteArrayInputStream(ByteArray(0))
            spec.standardOutput = LineLoggingOutputStream(logger, stdout)
            spec.errorOutput = LineLoggingOutputStream(logger, stderr)
            spec.environment(processEnvironment)
            workingDir?.let { spec.workingDir = it }
        }

        return CommandResult(
            exitValue = result.exitValue,
            stdout = stdout.toString(),
            stderr = stderr.toString(),
        )
    }

    private fun formatCommand(command: String, arguments: List<String>): String =
        (listOf(command) + arguments).joinToString(" ") { argument ->
            if (argument.any { it.isWhitespace() }) "'${argument.replace("'", "'\\''")}'" else argument
        }

    private data class CommandResult(
        val exitValue: Int,
        val stdout: String,
        val stderr: String,
    )
}

/** Captures process output while retaining the line-by-line logging users expect from Cargo. */
private class LineLoggingOutputStream(
    private val logger: Logger,
    private val output: StringBuilder,
) : OutputStream() {
    private val line = ByteArrayOutputStream()

    override fun write(value: Int) {
        line.write(value)
        if (value == '\n'.code) flushLine()
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        for (index in offset until offset + length) {
            write(bytes[index].toInt())
        }
    }

    override fun flush() {
        flushLine()
    }

    override fun close() {
        flushLine()
    }

    private fun flushLine() {
        if (line.size() == 0) return

        val text = String(line.toByteArray(), StandardCharsets.UTF_8)
        output.append(text)
        logger.lifecycle(text.trimEnd('\r', '\n'))
        line.reset()
    }
}

fun <T> withGlobalFileLock(lockFile: File, action: () -> T): T {
    RandomAccessFile(lockFile, "rw").use { raf ->
        val channel = raf.channel
        var lock: FileLock? = null
        while (lock == null) {
            try {
                lock = channel.tryLock()
            } catch (e: OverlappingFileLockException) {
                // Already locked in this JVM – wait and retry.
            }
            if (lock == null) {
                // tryLock() returns null, rather than throwing, when another process owns the lock.
                // Avoid busy-spinning while a potentially long-running operation holds it.
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw GradleException("Interrupted while waiting for file lock $lockFile", e)
                }
            }
        }
        lock.use { return action() }
    }
}

/**
 * Protect plugin-managed operations that write shared Cargo state, such as bindgen installation.
 * Normal Cargo builds rely on Cargo's own target-directory coordination; adding this lock around
 * every build would serialize otherwise independent Gradle module tasks.
 */
fun <T> withCargoTargetLock(targetDirectory: File, action: () -> T): T {
    val lockFile = targetDirectory.resolve(".uniffi-cargo.lock")
    lockFile.parentFile.mkdirs()
    return withGlobalFileLock(lockFile, action)
}
