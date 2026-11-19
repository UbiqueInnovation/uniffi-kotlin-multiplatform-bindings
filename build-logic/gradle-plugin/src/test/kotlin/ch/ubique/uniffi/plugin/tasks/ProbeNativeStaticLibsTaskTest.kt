package ch.ubique.uniffi.plugin.tasks

import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class ProbeNativeStaticLibsTaskTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `the probe keeps the library of the workspace build`() {
        val root = temporaryFolder.root.toPath()
        val fakeBin = root.resolve("fake-bin").createDirectories()
        val cargo = fakeBin.resolve("cargo")
        // Like Cargo, the probe replaces the library for the debug profile with its own build.
        cargo.writeText("""#!/bin/sh
            printf '%s\n' "${'$'}@" > "${'$'}CARGO_TARGET_DIR/probe-arguments"
            rm -f "${'$'}CARGO_TARGET_DIR/aarch64-apple-ios/debug/libsample.a"
            echo probe > "${'$'}CARGO_TARGET_DIR/aarch64-apple-ios/debug/libsample.a"
            echo 'note: native-static-libs: -lSystem -lc' >&2
        """.trimIndent() + "\n")
        assertTrue(cargo.toFile().setExecutable(true))
        val library = root.resolve("target/aarch64-apple-ios/debug").createDirectories().resolve("libsample.a")
        library.writeText("workspace\n")
        root.resolve("settings.gradle").writeText("rootProject.name = 'probe-test'\n")
        root.resolve("build.gradle").writeText("""
            plugins { id 'ch.ubique.uniffi.plugin' apply false }
            tasks.register('probe', ch.ubique.uniffi.plugin.tasks.ProbeNativeStaticLibsTask) {
                cargoMetadata.set('{}')
                lockFile.from('Cargo.lock')
                libraryFileName.set('libsample.a')
                nativeStaticLibsFile.set(layout.buildDirectory.file('sample.native-static-libs'))
                packageDirectory.set(layout.projectDirectory)
                cargoTargetDirectory.set(layout.projectDirectory.dir('target'))
                targetString.set('aarch64-apple-ios')
                useCross.set(false)
                additionalEnvironment.set([:])
            }
        """.trimIndent())

        fun probe() = GradleRunner.create()
            .withProjectDir(root.toFile())
            .withPluginClasspath()
            .withEnvironment(System.getenv() + ("PATH" to "$fakeBin:${System.getenv("PATH")}"))
            .withArguments("probe")
            .build()
            .task(":probe")?.outcome

        assertEquals(TaskOutcome.SUCCESS, probe())
        assertEquals("workspace\n", library.readText())
        assertFalse(library.resolveSibling("libsample.a.uniffi-kept").exists())
        assertEquals("-lSystem -lc", root.resolve("build/sample.native-static-libs").readText())
        assertEquals(
            listOf(
                "rustc", "--lib", "--target", "aarch64-apple-ios", "--crate-type", "staticlib",
                "--", "--print", "native-static-libs",
            ),
            root.resolve("target/probe-arguments").readText().lines().filter(String::isNotEmpty),
        )

        // A new workspace build alone doesn't run the probe again, a dependency change does.
        library.writeText("workspace, rebuilt\n")
        assertEquals(TaskOutcome.UP_TO_DATE, probe())
        root.resolve("Cargo.lock").writeText("version = 4\n")
        assertEquals(TaskOutcome.SUCCESS, probe())
        assertEquals("workspace, rebuilt\n", library.readText())
    }
}
