package io.gitlab.trixnity.gradle.utils

import org.gradle.api.GradleException
import org.gradle.api.Project

internal object PluginUtils {
    fun ensurePluginIsApplied(project: Project, pluginName: String, id: String) {
        if (!project.plugins.hasPlugin(id)) {
            project.logger.error(requiredPluginMessage(pluginName, id))
            throw GradleException("No $pluginName Gradle plugin found")
        }
    }
}

private fun requiredPluginMessage(name: String, id: String): String {
    return """
        Please include the $name Gradle plugin in your build configuration.

        plugins {
          // ...
          id("$id")
          // ...
        }
    """.trimIndent()
}
