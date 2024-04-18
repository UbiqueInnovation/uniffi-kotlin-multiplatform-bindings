/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.cargo

import io.gitlab.trixnity.gradle.CargoHost
import io.gitlab.trixnity.gradle.cargo.rust.CrateType
import io.gitlab.trixnity.gradle.cargo.rust.profiles.BuiltInCargoProfile
import io.gitlab.trixnity.gradle.cargo.rust.profiles.CargoProfile
import io.gitlab.trixnity.gradle.cargo.rust.targets.RustTarget
import io.gitlab.trixnity.gradle.utils.command
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import java.io.File

/**
 * Represents a Cargo package.
 */
class CargoPackage(
    val project: Project,
    searchPath: Directory,
) {
    /**
     * The manifest file of the package.
     */
    val manifestFile: RegularFile = project.layout.projectDirectory.file(
        project.command("cargo").apply {
            additionalEnvironmentPath(CargoHost.Platform.current.defaultCargoInstallationDir)
            arguments("locate-project", "--message-format", "plain")
            workingDirectory(searchPath)
        }.run(
            captureStandardOutput = true,
            captureStandardError = true,
        ).apply {
            assertNormalExitValue()
        }.standardOutput!!.trim()
    )

    /**
     * The root directory of the package.
     */
    val root: Directory = project.layout.projectDirectory.dir(manifestFile.asFile.parent)

    /**
     * The metadata of the workspace containing the package.
     */
    private val metadata: CargoMetadata = run {
        val result = project.command("cargo").apply {
            additionalEnvironmentPath(CargoHost.Platform.current.defaultCargoInstallationDir)
            // TODO: support fine-grained feature selection
            arguments("metadata", "--format-version", "1", "--all-features")
            workingDirectory(root)
        }.run(
            captureStandardOutput = true,
            captureStandardError = true,
        ).apply {
            assertNormalExitValue()
        }
        CargoMetadata.fromJsonString(result.standardOutput!!)
    }

    /**
     * The metadata of the current package.
     */
    private val packageMetadata = metadata.packages.first {
        File(it.manifestPath).absolutePath == manifestFile.asFile.absolutePath
    }

    /**
     * The fully qualified ID of the package.
     */
    private val packageId = CargoPackageId(
        // This must be called after `cargo metadata is invoked, since cargo pkgid does not work without Cargo.toml, and
        // cargo metadata generates it when it is not present.
        project.command("cargo").apply {
            additionalEnvironmentPath(CargoHost.Platform.current.defaultCargoInstallationDir)
            arguments("pkgid")
            workingDirectory(root)
        }.run(
            captureStandardOutput = true,
            captureStandardError = true,
        ).apply {
            assertNormalExitValue()
        }.standardOutput!!.trim()
    )

    /**
     * The metadata of the library target of the current package.
     */
    private val libraryCrateMetadata = packageMetadata.targets.firstOrNull {
        it.kinds.any(CargoTargetKind::isNormalLibrary)
    } ?: throw GradleException("Package ${packageId.name} does not have a library target")

    /**
     * The lock file of the workspace.
     */
    val lockFile: RegularFile = root.dir(metadata.workspaceRoot).file("Cargo.lock")

    /**
     * The name of the package, as defined in Cargo.toml.
     */
    val name: String = packageId.name!!

    /**
     * The path of the source roots (e.g., `src/lib.rs`) of all library and build script targets of all resolved Cargo
     * dependencies. This includes the current package's source files as well.
     */
    val dependencyTargetRoots: List<File> = run {
        val packageById = metadata.packages.associateBy { it.id }
        val targetsToFind = arrayOf(
            CargoTargetKind.Library,
            CargoTargetKind.StaticLibrary,
            CargoTargetKind.DynamicLibrary,
            CargoTargetKind.BuildScript,
        )

        val dependencyRoots = metadata.resolvedDependency.nodes.map { node ->
            packageById[node.id]!!
        }.flatMap { pkg ->
            pkg.targets
                .filter { target -> targetsToFind.any(target.kinds::contains) }
                .map { target -> File(target.sourcePath) }
        }

        // Likely to be a one-element list
        val currentRoots = packageMetadata.targets
            .filter { target -> target.kinds.any { kind -> kind.isNormalLibrary() } }
            .map { target -> File(target.sourcePath) }

        dependencyRoots + currentRoots
    }

    /**
     * The name of the library crate of this package, as defined in Cargo.toml. This defaults to the package name
     * replaced with underscores if omitted.
     */
    val libraryCrateName: String = libraryCrateMetadata.name.replace('-', '_')

    /**
     * The crate types of the library crate of this package, as defined in Cargo.toml.
     */
    val libraryCrateTypes: Set<CrateType> = libraryCrateMetadata.crateTypes.toSet()

    /**
     * The directory where the build result is stored by the profile and the target triple.
     */
    fun outputDirectory(profile: CargoProfile = BuiltInCargoProfile.Dev, target: RustTarget? = null): Directory {
        val targetDirectory = root.dir(metadata.targetDirectory)
        val targetSpecificDirectory = if (target != null) targetDirectory.dir(target.rustTriple) else targetDirectory
        return targetSpecificDirectory.dir(profile.targetChildDirectoryName)
    }
}
