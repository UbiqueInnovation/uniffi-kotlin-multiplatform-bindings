package ch.ubique.uniffi.plugin.services

import ch.ubique.uniffi.plugin.model.CargoMetadata
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.InputDirectory
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.AutoCloseable
import java.nio.charset.Charset
import javax.inject.Inject

abstract class CargoMetadataParams : ValueSourceParameters {
    @get:InputDirectory
    abstract val packageDirectory: DirectoryProperty
}

abstract class CargoMetadataService : ValueSource<String, CargoMetadataParams> {

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val stdout = ByteArrayOutputStream()

        execOperations.exec {
            commandLine("cargo", "metadata", "--format-version", "1")
            workingDir = parameters.packageDirectory.asFile.get()
            standardOutput = stdout
        }

        return String(stdout.toByteArray(), Charset.defaultCharset())
    }
}