plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.buildconfig)
}

val bindgenInfo = parseBindgenCargoToml(
    rootProject.layout.projectDirectory.file("../bindgen/Cargo.toml").asFile
)

group = "net.folivo.uniffi"
description = "Gradle UniFFI Plugin"
version = bindgenInfo.version

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(plugin(libs.plugins.kotlin.jvm))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jna)
}

buildConfig {
    packageName = "net.folivo.uniffi.gradle"

    buildConfigField("String", "BINDGEN_VERSION", "\"${bindgenInfo.version}\"")
    buildConfigField("String", "BINDGEN_CRATE", "\"${bindgenInfo.name}\"")
    buildConfigField("String", "BINDGEN_BIN", "\"${bindgenInfo.binName}\"")

    forClass("DependencyVersions") {
        buildConfigField("String", "OKIO", "\"${libs.versions.okio.get()}\"")
        buildConfigField("String", "KOTLINX_ATOMICFU", "\"${libs.versions.kotlinx.atomicfu.get()}\"")
        buildConfigField("String", "KOTLINX_DATETIME", "\"${libs.versions.kotlinx.datetime.get()}\"")
        buildConfigField("String", "KOTLINX_COROUTINES", "\"${libs.versions.kotlinx.coroutines.get()}\"")
        buildConfigField("String", "JNA", "\"${libs.versions.jna.get()}\"")
    }
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    vcsUrl.set("https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings")
    website.set(vcsUrl)

    plugins {
        create("uniffi-kotlin-multiplatform") {
            id = "net.folivo.uniffi.kotlin.multiplatform"
            displayName = name
            implementationClass = "net.folivo.uniffi.gradle.UniFfiPlugin"
            description = "A plugin for generating UniFFI Kotlin Multiplatform bindings for Rust libraries."
            tags.addAll("uniffi", "kotlin", "kotlin-multiplatform")
        }
    }
}

tasks.publish {
    dependsOn(tasks.publishPlugins)
}
