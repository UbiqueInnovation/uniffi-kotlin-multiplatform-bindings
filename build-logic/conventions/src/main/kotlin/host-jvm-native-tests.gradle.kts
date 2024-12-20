import io.gitlab.trixnity.gradle.rustlink.hostNativeTarget
import org.gradle.accessors.dm.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.atomicfu")
}

kotlin {
    jvmToolchain(17)
    jvm()
    hostNativeTarget()
    sourceSets {
        // https://github.com/gradle/gradle/issues/15383 and https://github.com/gradle/gradle/issues/19813
        val libs = project.extensions.getByName("libs") as org.gradle.accessors.dm.LibrariesForLibs
        
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotest.assertions.core)
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports {
        junitXml.required.set(true)
    }
}
