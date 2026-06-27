import com.vanniktech.maven.publish.GradlePublishPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.shadow)
    `kotlin-dsl`
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

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        // Must stay at 2.0 (not higher): :conventions consumes this module and is compiled by
        // the Gradle-embedded Kotlin (2.0.21 in Gradle 8.14.4), which cannot read newer metadata.
        // The "language version 2.0 is deprecated" warning is the accepted price until the
        // embedded Kotlin is raised by bumping the Gradle wrapper.
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
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
