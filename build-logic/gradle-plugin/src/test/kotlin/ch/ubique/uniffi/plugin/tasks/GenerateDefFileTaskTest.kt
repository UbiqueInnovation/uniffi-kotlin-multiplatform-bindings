package ch.ubique.uniffi.plugin.tasks

import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class GenerateDefFileTaskTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `linker options are read from the native static libs file`() {
        val root = temporaryFolder.root.toPath()
        root.resolve("libsample.a").writeText("")
        root.resolve("headers").createDirectories()
        root.resolve("linker-opts").writeText("-framework Security -lc\n")
        root.resolve("no-linker-opts").writeText("")
        root.resolve("settings.gradle").writeText("rootProject.name = 'def-file-test'\n")
        root.resolve("build.gradle").writeText("""
            plugins { id 'ch.ubique.uniffi.plugin' apply false }
            ['linker-opts', 'no-linker-opts'].each { name ->
                tasks.register(name, ch.ubique.uniffi.plugin.tasks.GenerateDefFileTask) {
                    staticLibrary.set(layout.projectDirectory.file('libsample.a'))
                    nativeStaticLibsFile.set(layout.projectDirectory.file(name))
                    outputFile.set(layout.buildDirectory.file("${'$'}{name}.def"))
                    targetString.set('aarch64-apple-darwin')
                    headersDir.set(layout.projectDirectory.dir('headers'))
                }
            }
        """.trimIndent())

        GradleRunner.create()
            .withProjectDir(root.toFile())
            .withPluginClasspath()
            .withArguments("linker-opts", "no-linker-opts")
            .build()

        assertTrue("linkerOpts = -framework Security -lc" in root.resolve("build/linker-opts.def").readText())
        assertFalse("linkerOpts" in root.resolve("build/no-linker-opts.def").readText())
    }
}
