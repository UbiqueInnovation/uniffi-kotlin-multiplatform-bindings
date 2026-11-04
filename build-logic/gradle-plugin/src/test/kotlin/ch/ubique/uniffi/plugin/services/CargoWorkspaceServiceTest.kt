package ch.ubique.uniffi.plugin.services

import ch.ubique.uniffi.plugin.model.BuildTarget.RustTarget
import org.gradle.api.services.BuildServiceParameters
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CargoWorkspaceServiceTest {
    private fun service() = object : CargoWorkspaceService() {
        override fun getParameters(): BuildServiceParameters.None = error("unused")
    }

    @Test
    fun `features are combined per workspace and rust target`() {
        val providers = ProjectBuilder.builder().build().providers
        val service = service()
        fun add(workspace: String, target: RustTarget, vararg features: String) =
            service.addFeatures(providers.provider { workspace }, target, providers.provider { features.toSet() })

        add("/ws", RustTarget.X64LinuxGnu, "b/second", "a/first")
        add("/ws", RustTarget.X64LinuxGnu, "a/first", "c/third")
        add("/ws", RustTarget.Aarch64AppleIos, "a/ios")
        add("/other", RustTarget.X64LinuxGnu, "d/other")

        assertEquals(listOf("a/first", "b/second", "c/third"), service.features("/ws", RustTarget.X64LinuxGnu).toList())
        assertEquals(setOf("a/ios"), service.features("/ws", RustTarget.Aarch64AppleIos))
        assertEquals(emptySet(), service.features("/ws", RustTarget.X64AppleDarwin))
    }

    @Test
    fun `each invocation runs once and shares its failure`() {
        val service = service()
        var runs = 0

        service.runOnce(listOf("build", "a")) { runs++ }
        service.runOnce(listOf("build", "a")) { runs++ }
        assertEquals(1, runs)

        service.runOnce(listOf("build", "b")) { runs++ }
        assertEquals(2, runs)

        assertFailsWith<IllegalStateException> { service.runOnce("failing") { runs++; error("cargo failed") } }
        assertFailsWith<IllegalStateException> { service.runOnce("failing") { runs++ } }
        assertEquals(3, runs)
    }
}
