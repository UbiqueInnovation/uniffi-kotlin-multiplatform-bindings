import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.atomicfu")
}

// https://github.com/gradle/gradle/issues/15383
val libs = the<LibrariesForLibs>()

val generatedDir = layout.buildDirectory.dir("generated/uniffi")
val crateDir = layout.projectDirectory.dir("uniffi")

val crateTargetDir = crateDir.dir("target")
val crateTargetBindingsDir = crateTargetDir.dir("bindings")
val crateTargetLibDir = crateTargetDir.dir("debug")

val buildCrate = tasks.register<Exec>("buildCrate") {
    group = "uniffi"
    workingDir(crateDir)
    commandLine("cargo", "build")
}

val cleanCrate = tasks.register<Exec>("cleanCrate") {
    group = "uniffi"
    workingDir(crateDir)
    commandLine("cargo", "clean")
}

val buildBindings = tasks.register<Exec>("buildBindings") {
    group = "uniffi"
    workingDir(crateDir)
    commandLine("cargo", "run", "--bin", "uniffi-bindgen")
    dependsOn(buildCrate)
}

val copyBindings = tasks.register<Copy>("copyBindings") {
    group = "uniffi"
    from(crateTargetBindingsDir)
    into(generatedDir)
    dependsOn(buildBindings)
}

val cleanBindings = tasks.register<Delete>("cleanBindings") {
    group = "uniffi"
    delete(crateTargetBindingsDir)
}

val copyBinariesToProcessedRessources = tasks.register<Copy>("copyBinaries") {
    group = "uniffi"
    from(crateTargetLibDir)
    include("*.so")
    into(layout.buildDirectory.dir("processedResources/jvm/main/linux-x86-64"))
    dependsOn(buildCrate)
}

tasks.withType<ProcessResources> {
    dependsOn(copyBinariesToProcessedRessources)
}

tasks.withType<KotlinCompile> {
    dependsOn(copyBindings)
}

tasks.withType<CInteropProcess> {
    dependsOn(copyBinariesToProcessedRessources, copyBindings)
}

tasks.clean {
    dependsOn(cleanBindings, cleanCrate)
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    val hostOs = System.getProperty("os.name")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        hostOs.startsWith("Windows") -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        compilations.getByName("main") {
            cinterops {
                register("uniffi") {
                    val testName = project.name
                    packageName("$testName.cinterop")
                    header(generatedDir.map { it.file("nativeInterop/cinterop/headers/$testName/$testName.h") })
                    extraOpts("-libraryPath", crateTargetLibDir.asFile.absolutePath)
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(generatedDir.map { it.dir("commonMain/kotlin") })

            dependencies {
                implementation(libs.okio)
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotest.assertions.core)
            }
        }
        val jvmMain by getting {
            kotlin.srcDir(generatedDir.map { it.dir("jvmMain/kotlin") })
            dependencies {
                implementation(libs.jna)
            }
        }
        val jvmTest by getting
        val nativeMain by getting {
            kotlin.srcDir(generatedDir.map { it.dir("nativeMain/kotlin") })
        }
        val nativeTest by getting
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-Xexpect-actual-classes"
    }
}

tasks.withType<Test> {
    reports {
        junitXml.required.set(true)
    }
}
