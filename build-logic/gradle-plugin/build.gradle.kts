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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.tools.gradle)
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
    signAllPublications()
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
