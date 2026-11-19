package dev.oxikt.gradle.plugin

import dev.oxikt.gradle.plugin.dsl.CargoExtension
import dev.oxikt.gradle.plugin.dsl.UniffiExtension
import dev.oxikt.gradle.plugin.model.BuildTarget
import dev.oxikt.gradle.plugin.tasks.BuildBindingsTask
import dev.oxikt.gradle.plugin.tasks.CargoBuildTask
import dev.oxikt.gradle.plugin.tasks.GenerateDefFileTask
import dev.oxikt.gradle.plugin.tasks.MergeLibrariesTask
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class PrebuiltLibrariesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `prebuilt libraries are used instead of building them with cargo`() {
        val root = temporaryFolder.root.toPath()
        root.resolve("Cargo.toml").writeText(
            """
            [package]
            name = "sample"
            version = "0.1.0"
            """.trimIndent()
        )
        root.resolve("src").createDirectories().resolve("lib.rs").writeText("")
        root.resolve("gradle.properties").writeText("kotlin.mpp.enableCInteropCommonization=true\n")

        val project = ProjectBuilder.builder().withProjectDir(root.toFile()).build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply("dev.oxikt.gradle.plugin")
        project.extensions.getByType(UniffiExtension::class.java).generateFromLibrary()
        project.extensions.getByType(CargoExtension::class.java)
            .prebuiltLibraries.set(project.layout.projectDirectory.dir("prebuilt"))
        project.extensions.getByType(KotlinMultiplatformExtension::class.java).apply {
            jvm()
            macosArm64()
        }
        (project as ProjectInternal).evaluate()

        fun prebuilt(target: BuildTarget.RustTarget, file: String) =
            project.projectDir.resolve("prebuilt/${target.rustTriple}/debug/$file")
        fun Task.cargoBuilds() = taskDependencies.getDependencies(this).filterIsInstance<CargoBuildTask>()

        val host = BuildTarget.RustTarget.forCurrentPlatform
        val hostLibrary = prebuilt(host, host.dynamicLibraryName("sample")!!)

        val merge = project.tasks.getByName(Tasks.MERGE_JVM_RESOURCES) as MergeLibrariesTask
        assertEquals(listOf(hostLibrary), merge.libraries.get().flatMap { it.files.files })
        assertEquals(emptyList(), merge.cargoBuilds())

        val bindings = project.tasks.getByName(Tasks.BUILD_BINDINGS) as BuildBindingsTask
        assertEquals(hostLibrary, bindings.libraryFile.get().asFile)
        assertEquals(emptyList(), bindings.cargoBuilds())

        val macos = BuildTarget.RustTarget.Aarch64AppleDarwin
        val defFile = project.tasks.getByName(Tasks.generateDefFile(BuildTarget.MacosArm64)) as GenerateDefFileTask
        assertEquals(prebuilt(macos, macos.staticLibraryName("sample")!!), defFile.staticLibrary.get().asFile)
        assertEquals(prebuilt(macos, "sample.native-static-libs"), defFile.nativeStaticLibsFile.get().asFile)
        assertEquals(emptyList(), defFile.cargoBuilds())
    }
}
