import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

fun DependencyHandler.plugin(dependency: Provider<PluginDependency>): Dependency =
    dependency.get().run { create("$pluginId:$pluginId.gradle.plugin:$version") }
