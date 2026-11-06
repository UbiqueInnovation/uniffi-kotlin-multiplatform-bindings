package ch.ubique.uniffi.plugin

import ch.ubique.uniffi.plugin.dsl.BuildMode
import ch.ubique.uniffi.plugin.dsl.CargoExtension
import ch.ubique.uniffi.plugin.dsl.UniffiExtension
import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.tasks.CargoBuildTask
import ch.ubique.uniffi.plugin.tasks.GenerateDefFileTask
import ch.ubique.uniffi.plugin.tasks.ProbeNativeStaticLibsTask
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class WorkspaceBuildTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `projects in workspace mode build together, individual projects on their own`() {
        val root = temporaryFolder.root.toPath()
        root.resolve("Cargo.toml").writeText("[workspace]\nmembers = [\"a\", \"b\", \"c\"]\n")
        root.resolve("gradle.properties").writeText("kotlin.mpp.enableCInteropCommonization=true\n")
        for (name in listOf("a", "b", "c")) {
            val crate = root.resolve(name).createDirectories()
            crate.resolve("Cargo.toml").writeText(
                """
                [package]
                name = "$name"
                version = "0.1.0"

                [lib]
                crate-type = ["lib", "cdylib", "staticlib"]
                """.trimIndent()
            )
            crate.resolve("src").createDirectories().resolve("lib.rs").writeText("")
        }

        val rootProject = ProjectBuilder.builder().withProjectDir(root.toFile()).build()
        fun project(name: String, mode: BuildMode, native: Boolean): Project {
            val project = ProjectBuilder.builder()
                .withParent(rootProject)
                .withName(name)
                .withProjectDir(root.resolve(name).toFile())
                .build()
            project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            project.pluginManager.apply("ch.ubique.uniffi.plugin")
            project.extensions.getByType(UniffiExtension::class.java).generateFromLibrary()
            project.extensions.getByType(KotlinMultiplatformExtension::class.java).apply {
                jvm()
                if (native) iosArm64()
            }
            // Set after the targets, as build scripts often do.
            project.extensions.getByType(CargoExtension::class.java).buildMode.set(mode)
            return project
        }
        val a = project("a", BuildMode.Workspace, native = true)
        val b = project("b", BuildMode.Workspace, native = false)
        val c = project("c", BuildMode.Individual, native = true)
        listOf(a, b, c).forEach { (it as ProjectInternal).evaluate() }

        fun Project.cargoBuild(target: BuildTarget.RustTarget) =
            tasks.getByName(Tasks.cargoBuild(target, release = false)) as CargoBuildTask

        val host = BuildTarget.RustTarget.forCurrentPlatform
        val ios = BuildTarget.RustTarget.Aarch64AppleIos
        assertEquals(setOf("a", "b"), a.cargoBuild(host).workspacePackageNames.get())
        assertEquals(setOf("a", "b"), b.cargoBuild(host).workspacePackageNames.get())
        // b has no iOS target, and c builds on its own.
        assertEquals(setOf("a"), a.cargoBuild(ios).workspacePackageNames.get())
        assertEquals(emptySet(), c.cargoBuild(host).workspacePackageNames.get())
        assertEquals(emptySet(), c.cargoBuild(ios).workspacePackageNames.get())

        // The def file of a workspace build reads the probed linker options, an individual build reports them.
        fun Project.defFile() = tasks.getByName(Tasks.generateDefFile(BuildTarget.IosArm64)) as GenerateDefFileTask
        val probe = a.tasks.getByName(Tasks.probeNativeStaticLibs(BuildTarget.IosArm64)) as ProbeNativeStaticLibsTask
        assertEquals(probe.nativeStaticLibsFile.get(), a.defFile().nativeStaticLibsFile.get())
        assertEquals(
            c.cargoBuild(ios).cargoOutputDirectory.get().file("c.native-static-libs").asFile,
            c.defFile().nativeStaticLibsFile.get().asFile,
        )
        assertEquals(c.defFile().nativeStaticLibsFile.get(), c.cargoBuild(ios).nativeStaticLibsFile.get())
    }
}
