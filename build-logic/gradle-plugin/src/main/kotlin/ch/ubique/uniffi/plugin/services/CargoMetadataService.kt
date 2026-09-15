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
            // The plugin only needs package/target information. Resolving the complete
            // dependency graph here makes every applied module update all registry and Git
            // dependencies, even though dependency resolution belongs to the actual Cargo
            // build tasks.
            spec.commandLine(
                cargoCommand.path,
                "metadata",
                "--no-deps",
                "--format-version",
                "1",
            )
            spec.workingDir = parameters.packageDirectory.asFile.get()
            // Do not allow a missing credential to turn configuration into an indefinite
            // interactive prompt if Cargo still needs to inspect a Git-based workspace member.
            spec.standardInput = ByteArrayInputStream(ByteArray(0))
            spec.environment("GIT_TERMINAL_PROMPT", "0")
            spec.standardOutput = stdout
        }

        return String(stdout.toByteArray(), Charset.defaultCharset())
    }
}
