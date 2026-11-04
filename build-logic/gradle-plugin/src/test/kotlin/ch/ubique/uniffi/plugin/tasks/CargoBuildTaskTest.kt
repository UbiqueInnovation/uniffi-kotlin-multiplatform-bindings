package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.model.CrateType
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    fun `crate type override is only passed when the declared crate types lack it`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(createTempDirectory("uniffi-cargo-crate-types").toFile())
            .build()
        fun override(requested: Set<CrateType>, declared: Set<CrateType>?): String? {
            val task = project.tasks.register(
                "cargoBuild${project.tasks.size}",
                CargoBuildTask::class.java,
            ).get()
            task.crateTypes.set(requested)
            declared?.let { task.declaredCrateTypes.set(it) }
            return task.crateTypeOverride.orNull
        }

        val all = setOf(CrateType.SystemDynamicLibrary, CrateType.SystemStaticLibrary, CrateType.Library)
        assertNull(override(setOf(CrateType.SystemDynamicLibrary), all))
        assertNull(override(setOf(CrateType.SystemStaticLibrary), all))
        assertNull(override(emptySet(), setOf(CrateType.Library)))
        assertEquals("cdylib", override(setOf(CrateType.SystemDynamicLibrary), setOf(CrateType.Library)))
        assertEquals("cdylib", override(setOf(CrateType.SystemDynamicLibrary), null))
        assertEquals(
            "cdylib,staticlib",
            override(
                setOf(CrateType.SystemStaticLibrary, CrateType.SystemDynamicLibrary),
                setOf(CrateType.SystemDynamicLibrary),
            ),
        )
    }

    @Test
    fun `features are passed to cargo`() {
        assertEquals("first,second", cargoFeatures("['second', 'first']"))
    }

    @Test
    fun `projects building the workspace share one cargo run`() {
        val root = createTempDirectory("uniffi-cargo-workspace")
        val fakeBin = root.resolve("fake-bin").createDirectories()
        val cargo = fakeBin.resolve("cargo")
        cargo.writeText("""#!/bin/sh
            mkdir -p "${'$'}CARGO_TARGET_DIR"
            echo invoked >> "${'$'}CARGO_TARGET_DIR/invocations"
            { pwd -P; printf '%s\n' "${'$'}@"; } > "${'$'}CARGO_TARGET_DIR/arguments"
        """.trimIndent() + "\n")
        assertTrue(cargo.toFile().setExecutable(true))
        root.resolve("settings.gradle").writeText("rootProject.name = 'cargo-workspace-test'\ninclude 'a', 'b'\n")
        root.resolve("a").createDirectories()
        root.resolve("b").createDirectories()
        root.resolve("build.gradle").writeText("""
            plugins { id 'ch.ubique.uniffi.plugin' apply false }
            def service = gradle.sharedServices.registerIfAbsent('cargoWorkspace', ch.ubique.uniffi.plugin.services.CargoWorkspaceService) {}
            subprojects {
                tasks.register('testCargoBuild', ch.ubique.uniffi.plugin.tasks.CargoBuildTask) {
                    packageDirectory.set(layout.projectDirectory)
                    workspaceDirectory.set(rootProject.layout.projectDirectory)
                    workspaceService.set(service)
                    usesService(service)
                    cargoTargetDirectory.set(rootProject.layout.projectDirectory.dir('target'))
                    packageName.set(project.name)
                    libraryName.set(project.name)
                    release.set(false)
                    useCross.set(false)
                    additionalEnvironment.set([:])
                    workspacePackageNames.set(['a', 'b'])
                    features.set(['b/second', 'a/first'])
                }
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(root.toFile())
            .withPluginClasspath()
            .withEnvironment(System.getenv() + ("PATH" to "${fakeBin}:${System.getenv("PATH")}"))
            .withArguments("testCargoBuild", "--parallel")
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":a:testCargoBuild")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":b:testCargoBuild")?.outcome)
        assertEquals(1, root.resolve("target/invocations").toFile().readLines().size)
        val arguments = root.resolve("target/arguments").toFile().readLines()
        assertEquals(root.toRealPath().toString(), arguments.first())
        assertEquals(
            listOf("build", "--lib", "--package", "a", "--package", "b", "--features", "a/first,b/second"),
            arguments.drop(1),
        )
    }

    @Test
    fun `a package built on its own only gets the features that apply to it`() {
        val project = ProjectBuilder.builder().build()
        fun packageFeatures(features: Set<String>, workspacePackages: Set<String>?): List<String> {
            val task = project.tasks.register("cargoBuild${project.tasks.size}", CargoBuildTask::class.java).get()
            task.packageName.set("sample")
            task.dependencyNames.set(setOf("dependency", "member-dependency"))
            task.features.set(features)
            workspacePackages?.let { task.workspacePackageNames.set(it) }
            return task.packageFeatures.get()
        }

        // Without a workspace build, the features are passed as configured and Cargo checks them.
        assertEquals(
            listOf("dependency/feature", "first", "other/feature", "sample/own"),
            packageFeatures(setOf("first", "", "sample/own", "dependency/feature", "other/feature"), null),
        )
        // A workspace build has the features of all projects. Only those of this package and of
        // its dependencies apply, including a workspace member it depends on.
        assertEquals(
            listOf("dependency/feature", "dependency?/weak", "member-dependency/feature", "sample/own"),
            packageFeatures(
                setOf(
                    "sample/own",
                    "dependency/feature",
                    "dependency?/weak",
                    "member-dependency/feature",
                    "other/feature",
                    "unrelated/feature",
                ),
                workspacePackages = setOf("sample", "other", "member-dependency"),
            ),
        )
    }

    /**
     * Runs a [CargoBuildTask] against a fake cargo and returns the value of the `--features`
     * argument it received, if any. [features] is a Groovy list literal.
     */
    private fun cargoFeatures(features: String): String? {
        val root = createTempDirectory("uniffi-cargo-features")
        val fakeBin = root.resolve("fake-bin").createDirectories()
        val cargo = fakeBin.resolve("cargo")
        cargo.writeText("""#!/bin/sh
            mkdir -p "${'$'}CARGO_TARGET_DIR"
            printf '%s\n' "${'$'}@" > "${'$'}CARGO_TARGET_DIR/arguments"
        """.trimIndent() + "\n")
        assertTrue(cargo.toFile().setExecutable(true))
        root.resolve("settings.gradle").writeText("rootProject.name = 'cargo-features-test'\n")
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
                features.set($features)
            }
        """.trimIndent())

        GradleRunner.create()
            .withProjectDir(root.toFile())
            .withPluginClasspath()
            .withEnvironment(System.getenv() + ("PATH" to "${fakeBin}:${System.getenv("PATH")}"))
            .withArguments("testCargoBuild")
            .build()

        return root.resolve("target/arguments").toFile().readLines()
            .dropWhile { it != "--features" }
            .getOrNull(1)
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
