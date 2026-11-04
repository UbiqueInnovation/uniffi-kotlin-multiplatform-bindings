package ch.ubique.uniffi.plugin.dsl

import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.model.CargoBuildConfig
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class CargoExtension(project: Project) {
    /**
     * Where the rust code is located
     */
    val packageDirectory: DirectoryProperty =
        project.objects.directoryProperty()
            .convention(project.layout.projectDirectory)

    /**
     * Cargo's shared target directory. When unset, the directory reported by `cargo metadata`
     * is used. Set this when several Gradle builds should reuse the same Cargo compilation
     * cache, for example:
     *
     * ```kotlin
     * cargo {
     *     targetDirectory = rootProject.layout.projectDirectory.dir("cargo-build")
     * }
     * ```
     */
    val targetDirectory: DirectoryProperty = project.objects.directoryProperty()

    /**
     * Libraries built outside of Gradle, used instead of building them with Cargo. Laid out like
     * Cargo's target directory, `<target triple>/<profile>/<library file>`. Kotlin/Native targets
     * also need the linker options for the library's native dependencies in
     * `<library name>.native-static-libs` next to it, as written by rustc's
     * `--print native-static-libs=<file>`.
     *
     * ```kotlin
     * cargo {
     *     prebuiltLibraries = rootProject.layout.projectDirectory.dir("prebuilt")
     * }
     * ```
     */
    val prebuiltLibraries: DirectoryProperty = project.objects.directoryProperty()

    /**
     * Build every library of the Cargo workspace that declares a `cdylib` or `staticlib` in
     * each Cargo invocation, instead of only this package.
     *
     * Cargo resolves features per invocation, so workspace libraries that depend on each other
     * see different feature sets of their shared dependencies depending on which package is
     * built. A library declaring a `cdylib` keeps a single build per target, so it is rebuilt
     * whenever the previous invocation built another package. Building the same set of packages
     * every time resolves features once for all of them: the first invocation builds, the
     * following ones find everything fresh.
     *
     * Enable it in every project of the workspace, e.g. from a `subprojects` block.
     */
    val buildWorkspaceLibraries: Property<Boolean> =
        project.objects.property(Boolean::class.java).convention(false)

    /** Optional compiler wrapper, such as `sccache`. */
    val rustcWrapper: Property<String> = project.objects.property(String::class.java).apply {
        convention(project.providers.environmentVariable("RUSTC_WRAPPER"))
    }

    /** Optional workspace compiler wrapper, such as `sccache`. */
    val rustcWorkspaceWrapper: Property<String> =
        project.objects.property(String::class.java).apply {
            convention(project.providers.environmentVariable("RUSTC_WORKSPACE_WRAPPER"))
        }

    /**
     * Cargo features to enable for every build of the crate, passed to Cargo as `--features`.
     * A name qualified with a package, `<package>/<feature>`, enables that dependency's feature,
     * or in a workspace build that member's.
     *
     * Features for some targets only go into [compilations] instead, for example:
     *
     * ```kotlin
     * cargo {
     *     features.add("logging")
     *     compilations.iosArm64 { features.add("ios-only") }
     * }
     * ```
     */
    val features: SetProperty<String> = project.objects.setProperty(String::class.java)

    /**
     * Android ABIs to compile for debug builds.
     *
     * An empty list keeps the existing host-based defaults. Release builds always compile all
     * supported ABIs unless a future release-specific option is added.
     */
    val androidDebugAbis: ListProperty<String> =
        project.objects.listProperty(String::class.java)

    /**
     * The Android NDK version to build the android targets with, e.g. `"28.1.13356709"`.
     *
     * `com.android.kotlin.multiplatform.library` has no `ndkVersion` of its own (it was
     * a property of the removed `com.android.build.gradle.BaseExtension`), and
     * `sdkComponents.ndkDirectory` fails with "NDK is not installed" unless AGP's
     * default version happens to be installed. When this is left unset the plugin
     * falls back to the newest NDK under `<sdk>/ndk`, then to `$ANDROID_NDK_ROOT`.
     */
    val ndkVersion: Property<String> = project.objects.property(String::class.java)

    val compilations: NamedDomainObjectContainer<CargoBuildConfig> =
        project.objects.domainObjectContainer(CargoBuildConfig::class.java).apply {
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
