import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.jvm)
}

// The `kotlin-dsl` plugin compiles the precompiled script plugins (incl. the special
// `compilePluginsBlocks` task) with language/api version 1.8, which Kotlin 2.4.0 no longer
// supports. Pin to KOTLIN_2_0 (mirrors :gradle-plugin) so the newer compiler accepts it.
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {
    implementation(project(":gradle-plugin"))
    implementation(libs.kotlin.gradle.plugin)
    // AGP is now compileOnly in :gradle-plugin (so it isn't bundled into the published jar), so
    // it no longer reaches here transitively. The convention script plugins apply the uniffi
    // plugin, whose UniffiPlugin references com.android.build.gradle.BaseExtension, so AGP must
    // be present on this build-logic classpath. This does not affect the published plugin jar.
    implementation(libs.android.tools.gradle)

    // https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}