import net.folivo.uniffi.gradle.InstallBindgenTask
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    kotlin("multiplatform")
    id("net.folivo.uniffi.kotlin.multiplatform")
}

uniffi {
    crateName = name
    crateDirectory = layout.projectDirectory.dir("uniffi")
    bindgenCratePath = rootProject.layout.projectDirectory.dir("bindgen")
}

kotlin {
    jvmToolchain(17)

    jvm()

    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    val arch = DefaultNativePlatform.getCurrentArchitecture()
    when {
        os.isMacOsX -> if (arch.isArm64) macosArm64() else macosX64()
        os.isLinux -> if (arch.isArm64) linuxArm64() else linuxX64()
        os.isWindows -> mingwX64()
        else -> throw GradleException("Unsupported os/arch pair: ${os.displayName}/${arch.displayName}.")
    }

    sourceSets {
        // https://github.com/gradle/gradle/issues/15383
        val libs = the<LibrariesForLibs>()

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

tasks.withType<InstallBindgenTask> {
    quiet = false
}
