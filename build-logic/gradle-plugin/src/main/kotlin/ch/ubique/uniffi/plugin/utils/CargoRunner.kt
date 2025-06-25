package ch.ubique.uniffi.plugin.utils

import org.gradle.api.logging.Logger
import java.io.File

class CargoRunner(
    private val logger: Logger,
    action: CargoRunner.() -> Unit = {}
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
        val builder = ProcessBuilder(listOf("cargo") + arguments)
        builder.redirectErrorStream(false)
        builder.environment().putAll(environment)
        workingDir?.let { builder.directory(it) }

        val process = builder.start()

        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        // Check if maybe just a target is missing and install it using rustup
        val rustupCommand = stderr.split('\n')
            .map {
                it.trim().substringAfter("help: consider downloading the target with `", "")
                    .substringBefore("`", "")
            }.firstOrNull(String::isNotEmpty)

        if (exitCode != 0 && rustupCommand != null) {
            logger.warn("Failed to run 'cargo ${arguments.joinToString(" ")}' trying to install rustup toolchain using '$rustupCommand'")

            val args = rustupCommand.split(' ')

            val builder = ProcessBuilder(args)
            builder.redirectErrorStream(true)
            builder.environment().putAll(environment)
            workingDir?.let { builder.directory(it) }

            val process = builder.start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            check(exitCode == 0) {
                println(output)
                logger.error("Failed to run '$rustupCommand'")
                "Failed to run command: '$rustupCommand' with exit code $exitCode"
            }

            // If the rustup command succeeded, retry the failed command.
            return this.run()
        }

        check(exitCode == 0) {
            println(stdout)
            println(stderr)
            logger.error("Failed to run 'cargo ${arguments.joinToString(" ")}'")
            "Failed to run 'cargo ${arguments.joinToString(" ")}' with exit code $exitCode"
        }

        return if (redirectErrorStream) {
            stdout + "\n" + stderr
        } else {
            stdout
        }
    }
}