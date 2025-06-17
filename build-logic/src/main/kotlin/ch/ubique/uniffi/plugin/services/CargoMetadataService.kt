package ch.ubique.uniffi.plugin.services

import ch.ubique.uniffi.plugin.model.CargoMetadata
import org.gradle.api.GradleException
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.File
import java.lang.AutoCloseable

abstract class CargoMetadataService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    fun getMetadata(packageDirectory: File): CargoMetadata {
        val process = ProcessBuilder(
            "cargo", "metadata", "--format-version", "1"
        ).directory(packageDirectory)
            // Sometimes it prints "Blocking waiting for file lock on package cache"
            // which would mess up the json parsing.
            .redirectErrorStream(false)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        check(exitCode == 0) {
            println(output)
            "Failed to run 'cargo metadata'!"
        }

        try {
            return CargoMetadata.fromJsonString(output)
        } catch (e: Exception) {
            println("Failed to parse output:")
            println(output)
            throw GradleException("Failed to get cargo metadata!")
        }
    }

    override fun close() {
        // nothing to clean up
    }
}