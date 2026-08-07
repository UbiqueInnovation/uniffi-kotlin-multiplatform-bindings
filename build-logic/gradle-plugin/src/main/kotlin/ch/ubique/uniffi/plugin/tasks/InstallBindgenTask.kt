package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.utils.BindgenSource
import ch.ubique.uniffi.plugin.utils.CargoRunner
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class InstallBindgenTask : DefaultTask() {
    @get:Input
    abstract val source: Property<BindgenSource>

    @get:Internal
    abstract val defaultBindgenBinName: Property<String>

    @get:OutputDirectory
    abstract val bindgenInstallPath: DirectoryProperty

    @get:Internal
    val bindgenBinPath: Provider<RegularFile> =
        bindgenInstallPath.file(
            source.map { it.bindgenName }
                .orElse(defaultBindgenBinName)
                .map { "bin/$it" }
        )

    @get:OutputDirectory
    abstract val bindgenBuildPath: DirectoryProperty

    @TaskAction
    fun action() {
        CargoRunner(logger) {
            argument("install")
            argument("--root")
            argument(bindgenInstallPath.asFile.get().path)
            argument("--force")

            val source = source.get()
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
    }
}