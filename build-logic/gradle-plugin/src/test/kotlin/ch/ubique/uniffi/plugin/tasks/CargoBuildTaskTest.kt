package ch.ubique.uniffi.plugin.tasks

import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.model.CrateType
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.gradle.testfixtures.ProjectBuilder

class CargoBuildTaskTest {
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
