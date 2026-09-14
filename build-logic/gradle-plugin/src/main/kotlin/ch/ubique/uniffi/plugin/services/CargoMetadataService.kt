package ch.ubique.uniffi.plugin.services

import ch.ubique.uniffi.plugin.utils.RustLocator
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.InputDirectory
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
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
        execOperations.exec { spec ->
            spec.commandLine(cargoCommand.path, "metadata", "--format-version", "1")
            spec.workingDir = parameters.packageDirectory.asFile.get()
            // Cargo may invoke Git while resolving workspace dependencies. Do not allow a
            // missing credential to turn configuration into an indefinite interactive prompt.
            spec.standardInput = ByteArrayInputStream(ByteArray(0))
            spec.environment("GIT_TERMINAL_PROMPT", "0")
            spec.standardOutput = stdout
        }

        return String(stdout.toByteArray(), Charset.defaultCharset())
    }
}
