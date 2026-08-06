import com.vanniktech.maven.publish.GradlePublishPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.shadow)
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.buildconfig)
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("kotlinx.serialization", "shadow.kotlinx.serialization")
    isZip64 = true
}

// This module is a published, standalone Gradle plugin: plain .kt sources, no
// precompiled script plugins. It therefore does NOT apply `kotlin-dsl`, which exists
// for build scripts and precompiled script plugins, and which pins the module to the
// Kotlin version embedded in the current Gradle release ("the `embedded-kotlin` and
// `kotlin-dsl` plugins rely on features of Kotlin X that might work differently than in
// the requested version Y"). The plugin sources use the plain Gradle API with explicit
// lambda parameters instead of the `org.gradle.kotlin.dsl` helpers, the same way AGP and
// the Kotlin Gradle Plugin are written.
//
// :conventions keeps `kotlin-dsl` - it has real precompiled script plugins, which is
// exactly what that plugin is for.
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Bundled into the shadow jar and relocated (see shadowJar block) so it can't clash
    // with the consumer project's kotlinx.serialization version.
    implementation(libs.kotlinx.serialization.json)

    // AGP and the Kotlin Gradle plugin are part of the consumer's buildscript classpath at
    // apply-time, so we only compile against them — we must NOT bundle them. Declaring them
    // as `implementation` pulls full, un-relocated copies of AGP/KGP (and their transitive
    // com.android.* / grpc / netty / bundletool classes) into the published uber jar, which
    // then shadow the consumer's real, newer AGP and cause failures such as
    // NoSuchMethodError ...ConfigurationOuterClass$Configuration$Builder.setSdkVersionMinor.
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.android.tools.gradle)
}

gradlePlugin {
    plugins {
        create("ch.ubique.uniffi.plugin") {
            id = "ch.ubique.uniffi.plugin"
            implementationClass = "ch.ubique.uniffi.plugin.UniffiPlugin"
        }
    }
}

group = property("GROUP").toString()
description = property("POM_DESCRIPTION").toString()
version = getProjectVersion()

tasks.publish {
    dependsOn(tasks.publishPlugins)
}

mavenPublishing {
    configure(GradlePublishPlugin())

    coordinates(property("GROUP").toString(), property("ARTIFACT_ID").toString(), project.version.toString())

    publishToMavenCentral(true)
    
    val disableSigning = hasProperty("disableSigning") && property("disableSigning") == "true"

    if (!disableSigning) {
        signAllPublications()
    }
}

private fun getProjectVersion(): String {
    val versionFromGradleProperties = property("VERSION").toString()
    val versionFromWorkflow = runCatching { property("githubRefName").toString().removePrefix("v") }.getOrNull()
    return versionFromWorkflow ?: versionFromGradleProperties
}

buildConfig {
    packageName = "ch.ubique.uniffi.plugin"

    forClass("PluginVersions") {
        buildConfigField("String", "RUNTIME_VERSION", "\"${version}\"")
    }
}
