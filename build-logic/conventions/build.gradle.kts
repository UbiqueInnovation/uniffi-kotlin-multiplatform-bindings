plugins {
    `kotlin-dsl`
    kotlin("jvm") version "2.1.0"
}

dependencies {
    implementation(plugin(libs.plugins.kotlin.multiplatform))
    implementation(plugin(libs.plugins.kotlin.atomicfu))

    implementation(project(":gradle-plugin"))

    // https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
