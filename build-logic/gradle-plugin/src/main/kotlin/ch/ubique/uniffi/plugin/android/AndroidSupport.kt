package ch.ubique.uniffi.plugin.android

import ch.ubique.uniffi.plugin.model.BuildTarget
import ch.ubique.uniffi.plugin.tasks.MergeLibrariesTask
import ch.ubique.uniffi.plugin.utils.NdkUtil
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * Every reference to the Android Gradle Plugin lives here, and nowhere else.
 *
 * AGP is a `compileOnly` dependency of this plugin - it is expected to come from the
 * consumer's buildscript classpath, and a consumer that does not build for android will
 * not have it at all. A class is only loaded when it is first used, so keeping the AGP
 * types out of [ch.ubique.uniffi.plugin.UniffiPlugin] and confining them to this class
 * means they can only ever be loaded once something instantiates it, which callers do
 * exclusively from inside `pluginManager.withPlugin(ANDROID_PLUGIN) { }` or behind
 * [BuildTarget.RustTarget.isAndroid].
 */
internal class AndroidSupport(private val project: Project) {

    private val androidComponents: KotlinMultiplatformAndroidComponentsExtension
        get() = project.extensions
            .getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)

    /**
     * The environment variables needed to build [rustTarget] with the NDK toolchain, or an
     * empty environment when `cross` is used - it brings its own toolchain and configures
     * it itself.
     *
     * Nothing is resolved here: [NdkUtil.ndkEnvVariables] throws when the NDK is missing,
     * so it must only run when the cargo task that needs it actually executes.
     */
    fun ndkEnvironment(
        rustTarget: BuildTarget.RustTarget,
        useCross: Provider<Boolean>,
        ndkVersion: Provider<String>,
    ): Provider<Map<String, String>> {
        val ndkEnvironment = androidComponents.sdkComponents.sdkDirectory
            .zip(ndkVersion.orElse("")) { sdk, version ->
                NdkUtil.ndkEnvVariables(
                    sdkRoot = sdk.asFile,
                    // The KMP android extension's minSdk is not readable this early in the
                    // lifecycle; 21 matches what :runtime declares.
                    apiLevel = 21,
                    ndkVersion = version.takeIf(String::isNotEmpty),
                    ndkRoot = null,
                    rustTriple = rustTarget.rustTriple,
                    ndkLlvmTriple = rustTarget.ndkLlvmTriple,
                )
            }

        val empty = project.providers.provider { emptyMap<String, String>() }
        return useCross.flatMap { cross -> if (cross) empty else ndkEnvironment }
    }

    /**
     * Hands the merged libraries to AGP.
     */
    fun wireVariants(
        jniLibrariesTask: TaskProvider<MergeLibrariesTask>,
        hostLibrariesTask: TaskProvider<MergeLibrariesTask>,
    ) {
        androidComponents.onVariants { variant ->
            variant.sources.jniLibs?.addGeneratedSourceDirectory(
                jniLibrariesTask,
                MergeLibrariesTask::outputDirectory
            )

            // Empty unless the build script opts in with `withHostTest { }`.
            variant.hostTests.forEach { (_, hostTest) ->
                hostTest.sources.resources?.addGeneratedSourceDirectory(
                    hostLibrariesTask,
                    MergeLibrariesTask::outputDirectory
                )
            }
        }
    }
}
