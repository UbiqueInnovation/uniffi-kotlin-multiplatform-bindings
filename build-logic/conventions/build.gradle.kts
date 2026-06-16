plugins {
    `kotlin-dsl`
    alias(libs.plugins.kotlin.jvm)
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