package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.model.CrateType
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testfixtures.ProjectBuilder

class CargoBuildTaskTest {
    @Test
    fun `cargo runs again while unchanged binding inputs stay up to date`() {
        val root = createTempDirectory("uniffi-cargo-repeat")
        val fakeBin = root.resolve("fake-bin").createDirectories()
        val cargo = fakeBin.resolve("cargo")
        cargo.writeText("""#!/bin/sh
            mkdir -p "${'$'}CARGO_TARGET_DIR/debug"
            echo invoked >> "${'$'}CARGO_TARGET_DIR/invocations"
            test -e "${'$'}CARGO_TARGET_DIR/debug/library.so" || touch "${'$'}CARGO_TARGET_DIR/debug/library.so"
            test -e "${'$'}CARGO_TARGET_DIR/debug/library.a" || touch "${'$'}CARGO_TARGET_DIR/debug/library.a"
        """.trimIndent() + "\n")
        assertTrue(cargo.toFile().setExecutable(true))
        root.resolve("settings.gradle").writeText("rootProject.name = 'cargo-repeat-test'\n")
        root.resolve("Cargo.toml").writeText("[package]\nname = \"sample\"\nversion = \"0.1.0\"\n")
        root.resolve("src").createDirectories().resolve("lib.rs").writeText("pub fn sample() {}\n")
        root.resolve("build.gradle").writeText("""
            plugins { id 'ch.ubique.uniffi.plugin' apply false }
            tasks.register('testCargoBuild', ch.ubique.uniffi.plugin.tasks.CargoBuildTask) {
                packageDirectory.set(layout.projectDirectory)
                cargoTargetDirectory.set(layout.projectDirectory.dir('target'))
                dynamicLibraryFile.set(layout.projectDirectory.file('target/debug/library.so'))
                staticLibraryFile.set(layout.projectDirectory.file('target/debug/library.a'))
                packageName.set('sample')
                libraryName.set('sample')
                release.set(false)
                useCross.set(false)
                additionalEnvironment.set([:])
            }
            tasks.register('testBindings') {
                dependsOn tasks.named('testCargoBuild')
                inputs.file(tasks.named('testCargoBuild').flatMap { it.dynamicLibraryFile })
                outputs.file(layout.projectDirectory.file('bindings.txt'))
                doLast { file('bindings.txt').text = 'generated' }
            }
        """.trimIndent())

        val environment = System.getenv() + ("PATH" to "${fakeBin}:${System.getenv("PATH")}")
        fun run() = GradleRunner.create()
            .withProjectDir(root.toFile())
            .withPluginClasspath()
            .withEnvironment(environment)
            .withArguments("testBindings")
            .build()

        val first = run()
        assertEquals(TaskOutcome.SUCCESS, first.task(":testCargoBuild")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, first.task(":testBindings")?.outcome)
        val second = run()
        assertEquals(TaskOutcome.SUCCESS, second.task(":testCargoBuild")?.outcome)
        assertEquals(TaskOutcome.UP_TO_DATE, second.task(":testBindings")?.outcome)
        assertEquals(2, root.resolve("target/invocations").toFile().readLines().size)
    }

    @Test
    fun `library outputs use Cargo shared target layout`() {
        val projectDirectory = createTempDirectory("uniffi-cargo-build-task-test")
        val project = ProjectBuilder.builder()
            .withProjectDir(projectDirectory.toFile())
            .build()
        val task = project.tasks.register("cargoBuild", CargoBuildTask::class.java).get()

        task.cargoTargetDirectory.set(project.layout.projectDirectory.dir("shared-target"))
        task.rustTarget.set(BuildTarget.RustTarget.X64LinuxGnu)
        task.release.set(false)
        task.packageName.set("sample-package")
        task.libraryName.set("sample_crate")
        task.useCross.set(false)
        task.crateTypes.add(CrateType.SystemDynamicLibrary)

        val cargoOutputDirectory = project.layout.projectDirectory.dir(
            "shared-target/x86_64-unknown-linux-gnu/debug",
        ).asFile.toPath()
        assertEquals(
            cargoOutputDirectory.resolve("libsample_crate.so"),
            task.dynamicLibraryFile.get().asFile.toPath(),
        )
        assertEquals(
            cargoOutputDirectory.resolve("libsample_crate.a"),
            task.staticLibraryFile.get().asFile.toPath(),
        )
        assertFalse(task.dynamicLibraryFile.get().asFile.path.contains("/build/uniffi/"))
    }
}
