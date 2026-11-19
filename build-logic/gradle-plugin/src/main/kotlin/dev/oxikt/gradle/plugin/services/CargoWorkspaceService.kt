package dev.oxikt.gradle.plugin.services

import dev.oxikt.gradle.plugin.model.BuildTarget
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Gives the projects of a build one view of each workspace build, see
 * [dev.oxikt.gradle.plugin.dsl.BuildMode.Workspace]: the packages to build and the features to
 * enable for each Rust target, and a single Cargo run that the others reuse.
 */
abstract class CargoWorkspaceService : BuildService<BuildServiceParameters.None> {
    private class Project(
        val workspaceRoot: Provider<String>,
        val rustTarget: BuildTarget.RustTarget,
        val packageName: Provider<String>,
        val features: Provider<Set<String>>,
    )

    private val projects = ConcurrentLinkedQueue<Project>()

    private val builds = ConcurrentHashMap<Any, Lazy<Result<Unit>>>()

    /**
     * Adds a project that builds [packageName] for [rustTarget] in the workspace at [workspaceRoot],
     * with [features]. An absent [packageName] leaves the project out, for a project that builds
     * its crate on its own. Called while the projects are configured, so the workspace builds are
     * complete once tasks read them.
     */
    fun addProject(
        workspaceRoot: Provider<String>,
        rustTarget: BuildTarget.RustTarget,
        packageName: Provider<String>,
        features: Provider<Set<String>>,
    ) {
        projects += Project(workspaceRoot, rustTarget, packageName, features)
    }

    /** The packages the projects build for [rustTarget] in the workspace at [workspaceRoot]. */
    fun packages(workspaceRoot: String, rustTarget: BuildTarget.RustTarget): Set<String> =
        projects(workspaceRoot, rustTarget).mapTo(sortedSetOf()) { it.packageName.get() }

    /** The features the projects enable for [rustTarget] in the workspace at [workspaceRoot]. */
    fun features(workspaceRoot: String, rustTarget: BuildTarget.RustTarget): Set<String> =
        projects(workspaceRoot, rustTarget).flatMapTo(sortedSetOf()) { it.features.get() }

    private fun projects(workspaceRoot: String, rustTarget: BuildTarget.RustTarget) = projects.filter {
        it.rustTarget == rustTarget && it.packageName.isPresent && it.workspaceRoot.get() == workspaceRoot
    }

    /**
     * Runs [build] for the first task with this [invocation] in the build. The others wait for it
     * and share its outcome, a failure included, instead of running Cargo again.
     */
    fun runOnce(invocation: Any, build: () -> Unit) {
        builds.computeIfAbsent(invocation) { lazy { runCatching(build) } }.value.getOrThrow()
    }
}
