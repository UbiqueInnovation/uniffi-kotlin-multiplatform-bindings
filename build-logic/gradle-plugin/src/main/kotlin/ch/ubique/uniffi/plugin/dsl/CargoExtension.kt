package ch.ubique.uniffi.plugin.dsl

import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.model.CargoBuildConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

abstract class CargoExtension(project: Project) {
    /**
     * Where the rust code is located
     */
    val packageDirectory: DirectoryProperty =
        project.objects.directoryProperty()
            .convention(project.layout.projectDirectory)

    /**
     * The Android NDK version to build the android targets with, e.g. `"28.1.13356709"`.
     *
     * `com.android.kotlin.multiplatform.library` has no `ndkVersion` of its own (it was
     * a property of the removed `com.android.build.gradle.BaseExtension`), and
     * `sdkComponents.ndkDirectory` fails with "NDK is not installed" unless AGP's
     * default version happens to be installed. When this is left unset the plugin
     * falls back to the newest NDK under `<sdk>/ndk`, then to `$ANDROID_NDK_ROOT`.
     */
    val ndkVersion: Property<String> = project.objects.property<String>()

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
