package ch.ubique.uniffi.plugin.services

import ch.ubique.uniffi.plugin.utils.RustLocator
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.InputDirectory
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
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
        val cargoCommand = RustLocator.findRustExecutable("cargo")
        execOperations.exec {
            commandLine(cargoCommand.path, "metadata", "--format-version", "1")
            workingDir = parameters.packageDirectory.asFile.get()
            standardOutput = stdout
        }

        return String(stdout.toByteArray(), Charset.defaultCharset())
    }
}