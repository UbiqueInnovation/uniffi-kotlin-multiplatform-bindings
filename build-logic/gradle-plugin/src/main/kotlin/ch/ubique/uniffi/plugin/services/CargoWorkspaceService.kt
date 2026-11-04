package ch.ubique.uniffi.plugin.services

import ch.ubique.uniffi.plugin.model.BuildTarget
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Gives the projects of a build one view of each workspace build, see
 * [ch.ubique.uniffi.plugin.dsl.CargoExtension.buildWorkspaceLibraries]: the features all of them
 * enable, and a single Cargo run that the others reuse.
 */
abstract class CargoWorkspaceService : BuildService<BuildServiceParameters.None> {
    private class Features(
        val workspaceRoot: Provider<String>,
        val rustTarget: BuildTarget.RustTarget,
        val features: Provider<Set<String>>,
    )

    private val features = ConcurrentLinkedQueue<Features>()

    private val builds = ConcurrentHashMap<Any, Lazy<Result<Unit>>>()

    /**
     * Adds the [features] a project enables for [rustTarget] in the workspace at [workspaceRoot].
     * Called while the projects are configured, so the union is complete once tasks read it.
     */
    fun addFeatures(
        workspaceRoot: Provider<String>,
        rustTarget: BuildTarget.RustTarget,
        features: Provider<Set<String>>,
    ) {
        this.features += Features(workspaceRoot, rustTarget, features)
    }

    /** The features all projects enable for [rustTarget] in the workspace at [workspaceRoot]. */
    fun features(workspaceRoot: String, rustTarget: BuildTarget.RustTarget): Set<String> = features
        .filter { it.rustTarget == rustTarget && it.workspaceRoot.get() == workspaceRoot }
        .flatMapTo(sortedSetOf()) { it.features.get() }

    /**
     * Runs [build] for the first task with this [invocation] in the build. The others wait for it
     * and share its outcome, a failure included, instead of running Cargo again.
     */
    fun runOnce(invocation: Any, build: () -> Unit) {
        builds.computeIfAbsent(invocation) { lazy { runCatching(build) } }.value.getOrThrow()
    }
}
