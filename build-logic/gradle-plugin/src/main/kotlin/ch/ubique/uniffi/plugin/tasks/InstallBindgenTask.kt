package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.utils.BindgenSource
import ch.ubique.uniffi.plugin.utils.CargoRunner
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
@CacheableTask
abstract class InstallBindgenTask : DefaultTask() {

    @get:OutputDirectory
    abstract val bindgenPath: DirectoryProperty

    @get:OutputDirectory
    abstract val bindgenTmpPath: DirectoryProperty

    @get:Input
    abstract val source: Property<BindgenSource>

    @TaskAction
    fun action() {
        CargoRunner(logger) {
            argument("install")
            argument("--root")
            argument(bindgenPath.asFile.get().path)
            argument("--force")

            val source = source.get()
            when (source) {
                is BindgenSource.Path -> {
                    argument("--path")
                    argument(source.path)
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

            env("CARGO_TARGET_DIR", bindgenTmpPath.asFile.get().path)
        }.run()
    }
}