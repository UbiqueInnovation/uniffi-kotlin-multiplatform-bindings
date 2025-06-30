package ch.ubique.uniffi.plugin.dsl

import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.model.CargoBuildConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty

abstract class CargoExtension(project: Project) {
    /**
     * Where the rust code is located
     */
    val packageDirectory: DirectoryProperty =
        project.objects.directoryProperty()
            .convention(project.layout.projectDirectory)

    val compilations: NamedDomainObjectContainer<CargoBuildConfig> =
        project.container<CargoBuildConfig>(CargoBuildConfig::class.java).apply {
            BuildTarget.RustTarget.entries.forEach {
                maybeCreate(it.name)
            }
        }

    fun NamedDomainObjectContainer<CargoBuildConfig>.iosArm64(action: CargoBuildConfig.() -> Unit) {
        named("Aarch64AppleIos", action)
    }

    fun NamedDomainObjectContainer<CargoBuildConfig>.iosX64(action: CargoBuildConfig.() -> Unit) {
        named("X64AppleIos", action)
    }

    fun NamedDomainObjectContainer<CargoBuildConfig>.macosArm64(action: CargoBuildConfig.() -> Unit) {
        named("Aarch64AppleDarwin", action)
    }

    fun NamedDomainObjectContainer<CargoBuildConfig>.macosX64(action: CargoBuildConfig.() -> Unit) {
        named("X64AppleDarwin", action)
    }


    fun NamedDomainObjectContainer<CargoBuildConfig>.linuxArm64(action: CargoBuildConfig.() -> Unit) {
        named("Aarch64LinuxGnu", action)
    }

    fun NamedDomainObjectContainer<CargoBuildConfig>.linuxX64(action: CargoBuildConfig.() -> Unit) {
        named("X64LinuxGnu", action)
    }

    fun NamedDomainObjectContainer<CargoBuildConfig>.windowsX64(action: CargoBuildConfig.() -> Unit) {
        named("X64WindowsGnu", action)
    }

    fun NamedDomainObjectContainer<CargoBuildConfig>.androidArm64(action: CargoBuildConfig.() -> Unit) {
        named("Aarch64Android", action)
    }

    fun NamedDomainObjectContainer<CargoBuildConfig>.androidArmV7(action: CargoBuildConfig.() -> Unit) {
        named("ArmV7Android", action)
    }

    fun NamedDomainObjectContainer<CargoBuildConfig>.androidX64(action: CargoBuildConfig.() -> Unit) {
        named("X64Android", action)
    }
}
