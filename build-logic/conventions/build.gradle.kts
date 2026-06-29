plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(project(":gradle-plugin"))
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.tools.gradle)

    // https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}