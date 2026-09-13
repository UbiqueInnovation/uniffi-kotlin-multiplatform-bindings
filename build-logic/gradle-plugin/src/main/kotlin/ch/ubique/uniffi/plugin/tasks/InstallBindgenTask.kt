package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.utils.BindgenSource
import ch.ubique.uniffi.plugin.utils.CargoRunner
import ch.ubique.uniffi.plugin.utils.withCargoTargetLock
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@DisableCachingByDefault(because = "The rust toolchain and the resolved bindgen revision are not declared inputs")
abstract class InstallBindgenTask : DefaultTask() {
    @get:Input
    abstract val source: Property<BindgenSource>

    @get:Internal
    abstract val defaultBindgenBinName: Property<String>

    @get:Internal
    abstract val bindgenInstallPath: DirectoryProperty

    @get:Internal
    val bindgenBinPath: Provider<RegularFile> =
        bindgenInstallPath.file(
            source.map { it.bindgenName }
                .orElse(defaultBindgenBinName)
                .map { "bin/$it" }
        )

    @get:Internal
    abstract val bindgenBuildPath: DirectoryProperty

    @get:Internal
    abstract val bindgenSourcePath: Property<String>

    @TaskAction
    fun action() {
        val bindgenBinary = bindgenBinPath.get().asFile
        val bindgenInstallDirectory = bindgenInstallPath.asFile.get()
        val source = source.get()
        val sourceFingerprint = sourceFingerprint(source)
        val sourceFingerprintFile = bindgenInstallDirectory.resolve(".uniffi-source-fingerprint")

        withCargoTargetLock(bindgenBuildPath.asFile.get()) {
            // Recheck after acquiring the lock: sibling module tasks may have observed a missing
            // executable at the same time and then waited for the first installation to finish.
            if (bindgenBinary.isFile &&
                sourceFingerprint != null &&
                sourceFingerprintFile.isFile &&
                sourceFingerprintFile.readText() == sourceFingerprint
            ) {
                logger.info("Reusing bindgen at $bindgenBinary")
                return@withCargoTargetLock
            }

            CargoRunner(logger) {
                argument("install")
                argument("--root")
                argument(bindgenInstallPath.asFile.get().path)
                argument("--force")

                when (source) {
                    is BindgenSource.Path -> {
                        argument("--path")
                        argument(source.path)

                        if (source.features.isNotEmpty()) {
                            argument("--features")
                            argument(source.features.joinToString(","))
                        }
                    }

                    is BindgenSource.Git -> {
                        argument("--git")
                        argument(source.repository)
                        when (source.commit) {
                            is BindgenSource.Git.Commit.Branch -> {
                                argument("--branch")
                                argument(source.commit.branch)
                            }

                            is BindgenSource.Git.Commit.Tag -> {
                                argument("--tag")
                                argument(source.commit.tag)
                            }

                            is BindgenSource.Git.Commit.Revision -> {
                                argument("--rev")
                                argument(source.commit.revision)
                            }

                            else -> {}
                        }
                    }

                    is BindgenSource.Registry -> {
                        argument("${source.packageName}@${source.version}")
                    }
                }

                source.bindgenName?.let {
                    argument("--bin")
                    argument(it)
                }

                source.packageName?.let {
                    argument(it)
                }

                env("CARGO_TARGET_DIR", bindgenBuildPath.asFile.get().path)
            }.run()

            // Write this only after a successful installation. A missing or stale marker forces
            // the next task to refresh the shared installation instead of trusting its binary.
            sourceFingerprint?.let {
                bindgenInstallDirectory.mkdirs()
                sourceFingerprintFile.writeText(it)
            }
        }
    }

    /**
     * Returns an identity that is safe to use for reuse decisions. Mutable sources deliberately
     * return null so that cargo install --force is run again instead of reusing a stale binary.
     */
    private fun sourceFingerprint(source: BindgenSource): String? = when (source) {
        is BindgenSource.Path -> fingerprintDirectory(File(bindgenSourcePath.get()))
        is BindgenSource.Registry -> source.cacheKey
        is BindgenSource.Git -> when (source.commit) {
            is BindgenSource.Git.Commit.Tag,
            is BindgenSource.Git.Commit.Revision -> source.cacheKey
            is BindgenSource.Git.Commit.Branch ->
                gitReferenceFingerprint(source, "refs/heads/${source.commit.branch}")
            null -> gitReferenceFingerprint(source, "HEAD")
        }
    }

    private fun gitReferenceFingerprint(source: BindgenSource.Git, reference: String): String? {
        val result = runCatching {
            val process = ProcessBuilder("git", "ls-remote", source.repository, reference)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (process.waitFor() != 0) return null
            output.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
                ?.substringBefore("\t")
                ?.takeIf { it.isNotEmpty() }
                ?.let { "${source.cacheKey}-$it" }
        }
        return result.getOrNull()
    }

    private fun fingerprintDirectory(directory: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        directory.walkTopDown()
            .onEnter { file -> file.name !in setOf(".git", "target", "build") }
            .filter { it.isFile }
            .sortedBy { it.relativeTo(directory).invariantSeparatorsPath }
            .forEach { file ->
                digest.update(file.relativeTo(directory).invariantSeparatorsPath.toByteArray(StandardCharsets.UTF_8))
                digest.update(0.toByte())
                file.inputStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead = input.read(buffer)
                    while (bytesRead >= 0) {
                        if (bytesRead > 0) digest.update(buffer, 0, bytesRead)
                        bytesRead = input.read(buffer)
                    }
                }
                digest.update(0.toByte())
            }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}
