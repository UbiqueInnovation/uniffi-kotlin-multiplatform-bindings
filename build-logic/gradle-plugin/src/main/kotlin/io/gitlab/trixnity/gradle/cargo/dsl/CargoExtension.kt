/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo.dsl

import io.gitlab.trixnity.gradle.RustHost
import io.gitlab.trixnity.gradle.Variant
import io.gitlab.trixnity.gradle.cargo.CargoPackage
import io.gitlab.trixnity.gradle.cargo.rust.targets.*
import io.gitlab.trixnity.gradle.rust.dsl.RustExtension
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.reflect.TypeOf
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.HasProject

abstract class CargoExtension(final override val project: Project) : HasProject, HasFeatures,
    HasVariants<CargoExtensionVariant>, HasJvmVariant, HasNativeVariant {
    /**
     * The package directory.
     */
    val packageDirectory: DirectoryProperty =
        project.objects.directoryProperty().convention(project.layout.projectDirectory)

    /**
     * The directory where `cargo` and `rustup` are installed. Defaults to `~/.cargo/bin`. If `RustExtension` is
     * present, uses [RustExtension.toolchainDirectory].
     */
    internal val toolchainDirectory = project.provider {
        project.extensions.findByType<RustExtension>()?.toolchainDirectory?.get()
            ?: RustHost.current.platform.defaultToolchainDirectory
    }

    /**
     * The parsed metadata and manifest of the package.
     */
    internal val cargoPackage: Provider<CargoPackage> =
        project.objects.property<CargoPackage>().value(
            packageDirectory.zip(toolchainDirectory) { pkg, toolchain ->
                CargoPackage(project, pkg, toolchain)
            },
        ).apply {
            disallowChanges()
            finalizeValueOnRead()
        }

    @Suppress("LeakingThis")
    final override val debug: CargoExtensionVariant =
        project.objects.newInstance(DefaultCargoExtensionVariant::class, Variant.Debug, this)

    @Suppress("LeakingThis")
    final override val release: CargoExtensionVariant =
        project.objects.newInstance(DefaultCargoExtensionVariant::class, Variant.Release, this)

    override val variants: Iterable<CargoExtensionVariant> = arrayListOf(debug, release)

    private val buildContainer = project.container<CargoBuild<CargoBuildVariant<RustTarget>>>()

    internal fun createOrGetBuild(rustTarget: RustTarget): DefaultCargoBuild<RustTarget, DefaultCargoBuildVariant<RustTarget, CargoBuild<*>>> {
        val build = buildContainer.findByName(rustTarget.friendlyName) ?: run {
            project.objects.newInstance(
                when (rustTarget) {
                    is RustAndroidTarget -> CargoAndroidBuild::class
                    is RustAppleMobileTarget -> CargoAppleMobileBuild::class
                    is RustPosixTarget -> CargoPosixBuild::class
                    is RustWindowsTarget -> CargoWindowsBuild::class
                },
                rustTarget,
                this,
            ).apply(buildContainer::add)
        }
        @Suppress("UNCHECKED_CAST")
        return build as DefaultCargoBuild<RustTarget, DefaultCargoBuildVariant<RustTarget, CargoBuild<*>>>
    }

    /**
     * The list of all available Cargo build command invocations.
     */
    val builds: CargoBuildCollection<CargoBuild<CargoBuildVariant<RustTarget>>> =
        CargoBuildCollectionImpl(buildContainer).apply {
            DslObject(this@CargoExtension).extensions.add(
                object :
                    TypeOf<CargoBuildCollection<CargoBuild<CargoBuildVariant<RustTarget>>>>() {},
                "builds",
                this,
            )
        }

    /**
     * The list of Android targets filtered by defaultConfig.ndk.abiFilters.
     */
    internal val androidTargetsToBuild: SetProperty<RustAndroidTarget> =
        project.objects.setProperty()

    /**
     * The variant of a target specified by the parent process like Xcode.
     */
    internal val nativeTargetVariantOverride: MapProperty<RustTarget, Variant> =
        project.objects.mapProperty()
}
