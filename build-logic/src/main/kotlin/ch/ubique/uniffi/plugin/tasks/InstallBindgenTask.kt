package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.Constants
import ch.ubique.uniffi.plugin.utils.BindgenSource
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class InstallBindgenTask : DefaultTask() {

    @OutputDirectory
    val bindgenPath = project.layout.buildDirectory.dir("bindgen-install")

    @OutputDirectory
    val bindgenTmpPath = project.rootProject.layout.buildDirectory.dir("bindgen-install/target")

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

        when (val source = source.get()) {
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

        arguments.add("--bin")
        arguments.add(Constants.BINDGEN_BIN_NAME)
        arguments.add(Constants.BINDGEN_PACKAGE_NAME)

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