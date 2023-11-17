import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess

plugins {
    kotlin("multiplatform")
    kotlin("plugin.atomicfu")
}

// https://github.com/gradle/gradle/issues/15383
val libs = the<LibrariesForLibs>()

val bindgenInstallDir = layout.buildDirectory.dir("bindgen-install")

val generatedDir = layout.buildDirectory.dir("generated/uniffi")
val crateDir = layout.projectDirectory.dir("uniffi")
val crateName = layout.projectDirectory.asFile.name

val crateTargetLibDir = rootProject.layout.projectDirectory.dir("target/debug")

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

val installBindgen = tasks.register<Exec>("installBindgen") {
    group = "uniffi"
    workingDir(crateDir)
    commandLine(
        "cargo",
        "install",
        "--root",
        bindgenInstallDir.get().asFile.canonicalPath,
        "--bins",
        "--path",
        rootProject.rootDir.canonicalFile
    )
}

val buildBindings = tasks.register<Task>("buildBindings") {
    group = "uniffi"

    doLast {
        exec {
            workingDir(crateDir)

            val library = crateTargetLibDir.asFileTree
                .matching {
                    include("lib$crateName.so", "lib$crateName.dylib", "$crateName.dll")
                }.elements.map { it.single() }

            commandLine(
                bindgenInstallDir.get().file("bin/uniffi-bindgen-kotlin-multiplatform"),
                "--out-dir",
                generatedDir.get(),
                "--lib-file",
                library.get(),
                "--crate",
                crateName,
                crateDir.file("src/$crateName.udl")
            )
        }.assertNormalExitValue()
    }

    dependsOn(buildCrate, installBindgen)
}

val cleanBindings = tasks.register<Delete>("cleanBindings") {
    group = "uniffi"
    delete(generatedDir)
}

val copyBinaries = tasks.register<Copy>("copyBinaries") {
    group = "uniffi"
    from(crateTargetLibDir)
    include("lib$crateName.so", "lib$crateName.dylib", "$crateName.dll")
    into(layout.buildDirectory.dir("processedResources/jvm/main/${com.sun.jna.Platform.RESOURCE_PREFIX}"))
    dependsOn(buildCrate)
}

tasks.withType<ProcessResources> {
    dependsOn(copyBinaries)
}

tasks.withType<KotlinCompile<*>> {
    dependsOn(buildBindings)
}

tasks.withType<CInteropProcess> {
    dependsOn(copyBinaries, buildBindings)
}

tasks.named<Delete>("clean") {
    dependsOn(cleanBindings, cleanCrate)
}

kotlin {
    jvmToolchain(17)

    jvm {
        withJava()
    }

    val hostOs = System.getProperty("os.name")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosArm64()
        hostOs == "Linux" -> linuxX64()
        hostOs.startsWith("Windows") -> mingwX64()
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

    targets.all {
        compilations.getByName("main") {
            kotlinOptions {
                freeCompilerArgs += "-Xexpect-actual-classes"
            }
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }

        commonMain {
            kotlin.srcDir(generatedDir.map { it.dir("commonMain/kotlin") })

            dependencies {
                implementation(libs.okio)
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.kotest.assertions.core)
            }
        }

        jvmMain {
            kotlin.srcDir(generatedDir.map { it.dir("jvmMain/kotlin") })
            dependencies {
                implementation(libs.jna)
            }
        }

        nativeMain {
            kotlin.srcDir(generatedDir.map { it.dir("nativeMain/kotlin") })
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports {
        junitXml.required.set(true)
    }
}
