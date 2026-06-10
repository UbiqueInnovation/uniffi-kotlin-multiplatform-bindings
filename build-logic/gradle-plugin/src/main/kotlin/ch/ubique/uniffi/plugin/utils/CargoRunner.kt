package ch.ubique.uniffi.plugin.utils

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import kotlin.concurrent.thread

class CargoRunner(
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
        val commandName = if (useCross) { "cross" } else { "cargo" }
        val command = RustLocator.findRustExecutable(commandName).path

        val builder = ProcessBuilder(listOf(command) + arguments)
        builder.redirectErrorStream(false)
        builder.environment().putAll(environment)
        workingDir?.let { builder.directory(it) }

        val process = runCatching { builder.start() }.getOrNull()
			?: throw GradleException("Failed to start $commandName. Is rust installed?")
        
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val stdoutThread = thread(name = "$commandName-stdout") {
            process.inputStream.bufferedReader().forEachLine { line ->
                logger.lifecycle(line)
                synchronized(stdout) { stdout.appendLine(line) }
            }
        }
        val stderrThread = thread(name = "$commandName-stderr") {
            process.errorStream.bufferedReader().forEachLine { line ->
                logger.lifecycle(line)
                synchronized(stderr) { stderr.appendLine(line) }
            }
        }

        val exitCode = process.waitFor()
        stdoutThread.join()   // make sure both streams are fully drained
        stderrThread.join()

        // Check if maybe just a target is missing and install it using rustup
        val targetToInstall = stderr.lineSequence()
            .mapNotNull {
                Regex("""consider downloading the target with `rustup target add ([^`]+)`""")
                    .find(it)?.groupValues?.get(1)
            }
            .firstOrNull()

        if (exitCode != 0 && targetToInstall != null) {
            logger.warn("Failed to run '$command ${arguments.joinToString(" ")}' trying to install rustup toolchain using 'rustup target add $targetToInstall'")
            val rustup = RustLocator.findRustExecutable("rustup").path
            val builder = ProcessBuilder(listOf(rustup, "target", "add", targetToInstall))
            builder.redirectErrorStream(true)
            builder.environment().putAll(environment)
            workingDir?.let { builder.directory(it) }

            val lockFile = File(System.getProperty("java.io.tmpdir"), "ch.ubique.rustup.lock")

			val (output, exitCode) = withGlobalFileLock(lockFile) {
				val process = builder.start()
				val output = process.inputStream.bufferedReader().readText()
				val exitCode = process.waitFor()
				output to exitCode
			}
			check(exitCode == 0) {
				println(output)
				logger.error("Failed to run 'rustup target add $targetToInstall'")
				"Failed to run command: 'rustup target add $targetToInstall' with exit code $exitCode"
			}

			// If the rustup command succeeded, retry the failed command.
			return this.run()
        }

        check(exitCode == 0) {
            println(stdout.toString())
            println(stderr.toString())
            logger.error("Failed to run '$command ${arguments.joinToString(" ")}'")
            "Failed to run '$command ${arguments.joinToString(" ")}' with exit code $exitCode"
        }

        return if (redirectErrorStream) {
            stdout.toString() + "\n" + stderr.toString()
        } else {
            stdout.toString()
        }
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
                // Already locked in this JVM – wait and retry
                Thread.sleep(50)
            }
        }
        lock.use { return action() }
    }
}
