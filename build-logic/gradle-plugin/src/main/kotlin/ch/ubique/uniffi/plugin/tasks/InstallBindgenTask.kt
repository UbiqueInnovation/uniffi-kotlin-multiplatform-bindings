package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.Constants
import ch.ubique.uniffi.plugin.utils.BindgenSource
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

        val arguments = mutableListOf<String>()
        arguments.add("cargo")
        arguments.add("install")
        arguments.add("--root")
        arguments.add(bindgenPath.get().asFile.path)
        arguments.add("--force")

        val source = source.get()
        when (source) {
            is BindgenSource.Path -> {
                arguments.add("--path")
                arguments.add(source.path)
            }
            is BindgenSource.Git -> {
                arguments.add("--git")
                arguments.add(source.repository)
                when (source.commit) {
                    is BindgenSource.Git.Commit.Branch -> {
                        arguments.add("--branch")
                        arguments.add(source.commit.branch)
                    }
                    is BindgenSource.Git.Commit.Tag -> {
                        arguments.add("--tag")
                        arguments.add(source.commit.tag)
                    }
                    is BindgenSource.Git.Commit.Revision -> {
                        arguments.add("--rev")
                        arguments.add(source.commit.revision)
                    }
                    else -> {}
                }
            }
            is BindgenSource.Registry -> {
                arguments.add("${source.packageName}@${source.version}")
            }
        }

        source.bindgenName?.let {
            arguments.add("--bin")
            arguments.add(it)
        }

        source.packageName?.let {
            arguments.add(it)
        }

        val processBuilder = ProcessBuilder(arguments)
        processBuilder.redirectErrorStream(true)
        val env = processBuilder.environment()
        env.put("CARGO_TARGET_DIR", bindgenTmpPath.get().asFile.path)

        val process = processBuilder.start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        check(exitCode == 0) {
            println(output)
            "Install bindgen failed with exit code $exitCode"
        }

    }
}