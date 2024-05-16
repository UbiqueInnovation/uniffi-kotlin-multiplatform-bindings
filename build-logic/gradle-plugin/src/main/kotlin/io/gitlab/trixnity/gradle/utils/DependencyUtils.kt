/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package io.gitlab.trixnity.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

internal object DependencyUtils {
    fun configureEachDependentProjects(
        currentProject: Project,
        action: (Project) -> Unit,
    ) {
        // A set of projects known to be dependent on `currentProject`.
        val consumedDependentProjects = mutableSetOf(currentProject)
        // A partial inverse graph of the project dependency graph, only containing the part not connected to
        // `currentProject`.
        val unconsumedDirectDependentsByProject = currentProject.rootProject.allprojects.associateWith {
            mutableSetOf<Project>()
        }
        for (project in currentProject.rootProject.allprojects) {
            project.configurations.configureEach { configuration ->
                configuration.dependencies.configureEach { dependency ->
                    if (dependency is ProjectDependency) {
                        // If this dependency points to a project that is already consumed, this project is also
                        // (indirectly) dependent on currentProject.
                        if (consumedDependentProjects.contains(dependency.dependencyProject)) {
                            // Perform DFS starting at `project`.
                            val stack = arrayListOf(project)
                            while (stack.isNotEmpty()) {
                                val stackItem = stack.removeLast()
                                // Visit `stackItem` if not visited.
                                if (!consumedDependentProjects.contains(stackItem)) {
                                    consumedDependentProjects.add(stackItem)
                                    action(stackItem)
                                    // Consume items in the inverse graph as well.
                                    stack.addAll(unconsumedDirectDependentsByProject[stackItem]!!)
                                    unconsumedDirectDependentsByProject[stackItem]!!.clear()
                                }
                            }
                        } else {
                            // Otherwise, just store the dependency for future use.
                            unconsumedDirectDependentsByProject[dependency.dependencyProject]!!.add(project)
                        }
                    }
                }
            }
        }
    }
}