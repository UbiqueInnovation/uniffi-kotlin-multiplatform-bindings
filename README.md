# UniFFI Kotlin Multiplatform bindings

[![License](https://img.shields.io/gitlab/license/trixnity/uniffi-kotlin-multiplatform-bindings)](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings/-/blob/main/LICENSE)
[![Crates.io](https://img.shields.io/crates/v/uniffi_bindgen_kotlin_multiplatform)](https://crates.io/crates/uniffi_bindgen_kotlin_multiplatform)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.gitlab.trixnity.uniffi.kotlin.multiplatform)](https://plugins.gradle.org/plugin/io.gitlab.trixnity.uniffi.kotlin.multiplatform)
[![Gitlab Build Status](https://img.shields.io/gitlab/pipeline-status/trixnity%2Funiffi-kotlin-multiplatform-bindings)](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings/-/pipelines/latest)

This project contains Kotlin Multiplatform bindings generation for [UniFFI](https://github.com/mozilla/uniffi-rs).

Currently only the Kotlin targets JVM and Native are supported.
JS support would be awesome, but needs WASM support within uniffi.

You can find examples on how to use the bindings in the [tests](./tests) directory.

# How to use

We recommend to first read the [UniFFI user guide](https://mozilla.github.io/uniffi-rs/).
If you follow their [tutorial](https://mozilla.github.io/uniffi-rs/Getting_started.html),
then you can use the Kotlin Multiplatform bindings as explained bellow during the
["Generating foreign-language bindings" part](https://mozilla.github.io/uniffi-rs/tutorial/foreign_language_bindings.html).

## Using the Gradle plugin

This project contains three Gradle plugins:

- The Cargo plugin (`io.gitlab.trixnity.cargo.kotlin.multiplatform`)
- The UniFFI plugin (`io.gitlab.trixnity.uniffi.kotlin.multiplatform`)
- The helper plugin for linking (`io.gitlab.trixnity.rustlink.kotlin.multiplatform`)

### The Cargo plugin

The Cargo plugin is responsible for building and linking the Rust library to your Kotlin project. You can use it even
when you are not using UniFFI. If the `Cargo.toml` is located in the project root, you can simply apply the
`io.gitlab.trixnity.cargo.kotlin.multiplatform` the plugin.

```kotlin
plugins {
    kotlin("multiplatform")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform") version "0.1.0"
}
```

If the Cargo package is located in another directory, you can configure the path in the `cargo {}` block.

```kotlin
cargo {
    // The Cargo package is located in a `rust` subdirectory.
    packageDirectory = layout.projectDirectory.dir("rust")
}
```

Since searching `Cargo.toml` is done
by [`cargo locate-project`](https://doc.rust-lang.org/cargo/commands/cargo-locate-project.html),
it still works even if you set `packageDirectory` to a subdirectory, but it is not recommended.

```kotlin
cargo {
    // This works
    packageDirectory = layout.projectDirectory.dir("rust/src")
}
```

If you want to use Cargo features or
customized [Cargo profiles](https://doc.rust-lang.org/cargo/reference/profiles.html),
you can configure them in the `cargo {}` block as well.

```kotlin
import io.gitlab.trixnity.gradle.cargo.rust.profiles.CargoProfile

cargo {
    features.addAll("foo", "bar")
    debug.profile = CargoProfile("my-debug")
    release.profile = CargoProfile.Bench
}
```

If you want to use different features for each variant (debug or release), you can configure them in the `debug {}` or
`release {}` blocks.

```kotlin
cargo {
    features.addAll("foo")
    debug {
        // Use "foo", "logging" for debug builds
        features.addAll("logging")
    }
    release {
        // Use "foo", "app-integrity-checks" for release builds
        features.addAll("app-integrity-checks")
    }
}
```

`features` are inherited from the outer block to the inner block. To override this behavior in the inner block,
use `.set()` or the `=` operator overloading.

```kotlin
cargo {
    features.addAll("foo")
    debug {
        // Use "foo", "logging" for debug builds
        features.addAll("logging")
    }
    release {
        // Use "app-integrity-checks" (not "foo"!) for release builds
        features.set(setOf("app-integrity-checks"))
    }
}
```

For configurations applied to all variants, you can use the `variants {}` block.

```kotlin
cargo {
    variants {
        features.addAll("another-feature")
    }
}
```

For Android and Apple platform builds invoked by Xcode, the plugin automatically decides which profile to use. For other
targets, you can configure it with the `jvmVariant` or `nativeVariant` properties. When undecidable, these values
default to `Variant.Debug`.

```kotlin
import io.gitlab.trixnity.gradle.Variant

cargo {
    jvmVariant = Variant.Release
    nativeVariant = Variant.Debug
}
```

Cargo build tasks are configured as the corresponding Kotlin target is added in the `kotlin {}` block. For example, if
you don't invoke `androidTarget()` in `kotlin {}`, the Cargo plugin won't configure the Android build task as well.

```kotlin
cargo {
    builds.android {
        println("foo") // not executed
    }
}

kotlin {
    // The plugin will react to the targets definition
    jvm()
    linuxX64()
}
```

The Cargo plugin scans all the Rust dependencies
using [`cargo metadata`](https://doc.rust-lang.org/cargo/commands/cargo-metadata.html). If you modify Rust source files
including those in dependencies defined in the Cargo manifest, the Cargo plugin will rebuild the Cargo project.

For Android builds, the Cargo plugin automatically determines the SDK and the NDK to use based on the property values of
the `android {}` block. To use different a NDK version, set `ndkVersion` to that version.

```kotlin
android {
    ndkVersion = "26.2.11394342"
}
```

The Cargo plugin also automatically determines the ABI to build based on the value
of `android.defaultConfig.ndk.abiFilters`. If you don't want to build for x86 or x86_64, set this
to `["arm64-v8a", "armeabi-v7a"]`.

```kotlin
android {
    defaultConfig {
        ndk.abiFilters += setOf("arm64-v8a", "armeabi-v7a")
    }
}
```

The Cargo plugin automatically configures environment variables like `ANDROID_HOME` or `CC_<target>` for you, but if you
need finer control, you can directly configure the properties of the build task. The build task is accessible in the
`builds {}` block.

```kotlin
import io.gitlab.trixnity.gradle.cargo.dsl.*

cargo {
    builds {
        // Configure Android builds
        android {
            debug.buildTaskProvider.configure {
                additionalEnvironment.put("CLANG", "/path/to/clang")
            }
        }
        // You can configure for other targets as well
        appleMobile {}
        desktop {}
        jvm {}
        mobile {}
        native {}
        posix {}
        mingw {}
        linux {}
        macos {}
        windows {}
    }
}
```

For JVM builds, the Cargo plugin tries to build all the targets, whether the required toolchains are installed on the
current system or not. The list of such targets by the build host is as follows.

| Targets      | Windows | macOS | Linux |
|--------------|---------|-------|-------|
| Android      | ✅       | ✅     | ✅     |
| Apple Mobile | ❌       | ✅     | ❌     |
| MinGW        | ✅       | ✅     | ✅     |
| macOS        | ❌       | ✅     | ❌     |
| Linux        | ✅       | ✅     | ✅     |
| Visual C++   | ✅       | ❌     | ❌     |

To build for specific targets only, you can configure that using the `jvm` property. For example, to build a shared
library for the current build host only, set this property to `rustTarget == CargoHost.current.hostTarget`.

```kotlin
import io.gitlab.trixnity.gradle.CargoHost
import io.gitlab.trixnity.gradle.cargo.dsl.*

cargo {
    builds.jvm {
        jvm = (rustTarget == CargoHost.current.hostTarget)
    }
}
```

Android local unit tests requires JVM targets to be built, as they run in the host machine's JVM. The Cargo plugin
automatically copies the Rust shared library targeting the host machine into Android local unit tests. It also finds
projects that depend on the project using the Cargo plugin, and the Rust library will be copied to all projects that
directly or indirectly use the Cargo project. If you want to include shared library built for a different platform, you
can control that using the `androidUnitTest` property.

```kotlin
import io.gitlab.trixnity.gradle.cargo.dsl.*
import io.gitlab.trixnity.gradle.cargo.rust.targets.RustWindowsTarget

cargo {
    builds.jvm {
        // Use Visual C++ X64 for Android local unit tests 
        androidUnitTest = (rustTarget == RustWindowsTarget.X64)
    }
}

kotlin {
    jvm()
    androidTarget()
}
```

Local unit tests are successfully built even if there are no builds with `androidUnitTest` enabled, but you will
encounter a runtime error when you invoke a Rust function from Kotlin.

When you build or publish your Rust Android library separately and run Android local unit tests in another build, you
also have to reference the JVM version of your library from the Android unit tests.

To build the JVM version, run the `<JVM target name>Jar` task. The name of the JVM target can be configured with the
`jvm()` function, which defaults to `"jvm"`. For example, when the name of the JVM target is `"desktop"`:

```kotlin
kotlin {
    jvm("desktop")
}
```

the name of the task will be `desktopJar`.

```shell
# ./gradlew :your:library:<JVM target name>Jar
./gradlew :your:library:desktopJar
```

The build output will be located in `build/libs/<project name>-<JVM target name>.jar`. In the above case, the name of
the JAR file will be `<project name>-desktop.jar`. The JAR file then can be referenced using the `files` or the
`fileTree` functions.

```kotlin
kotlin {
    sourceSets {
        getByName("androidUnitTest") {
            dependencies {
                // implementation(files("<project name>-<JVM target name>.jar"))
                implementation(files("library-desktop.jar"))
                implementation("net.java.dev.jna:jna:5.13.0") // required to run
            }
        }
    }
}
```

The above process can be automated using the `maven-publish` Gradle plugin. It publishes the JVM version of your library
separately. For more details about using `maven-publish` with Kotlin Multiplatform, please refer
[here](https://kotlinlang.org/docs/multiplatform-publish-lib.html).

To publish your library to the local Maven repository on your system, run the `publishToMavenLocal` task.

```shell
./gradlew :your:project:publishToMavenLocal
```

In the local repository which is located in `~/.m2`, you will see that multiple artifacts including `<project name>` and
`<project name>-<JVM target name>` are generated. To reference it, register the `mavenLocal()` repository and put the
artifact name to `implementation()`.

```kotlin
repositories {
    mavenLocal()
    // ...
}

kotlin {
    sourceSets {
        getByName("androidUnitTest") {
            dependencies {
                // implementation("<group name>:<project name>-<JVM target name>:<version>")
                implementation("your.library:library-desktop:0.1.0")
                implementation("net.java.dev.jna:jna:5.13.0") // required to run
            }
        }
    }
}
```

### The UniFFI plugin

The UniFFI plugin is responsible for generating Kotlin bindings from your Rust package. Here is an example of using the
UniFFI plugin to build bindings from the resulting library binary.

```kotlin
import io.gitlab.trixnity.gradle.Variant

plugins {
    kotlin("multiplatform")
    id("io.gitlab.trixnity.cargo.kotlin.multiplatform") version "0.1.0"
    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform") version "0.1.0"
}

uniffi {
    // Generate the bindings using library mode.
    generateFromLibrary {
        // The UDL namespace as in the UDL file. Defaults to the library crate name.
        namespace = "my_crate"
        // The name of the build that makes the library to use to generate the bindings. The list of the names can be
        // retrieved with `cargo.builds.names`. If not specified, the UniFFI plugin automatically selects a build.
        build = "AndroidArm64"
        // The variant of the build that makes the library to use. If unspecified, the UniFFI plugin automatically picks
        // one.
        variant = Variant.Debug
    }
}
```

If you want to generate bindings from a UDL file as well, you can specify the path using the `generateFromUdl {}` block.

```kotlin
uniffi {
    generateFromUdl {
        namespace = "..."
        build = "..."
        variant = Variant.Debug
        // The UDL file. Defaults to "${crateDirectory}/src/${crateName}.udl".
        udlFile = layout.projectDirectory.file("rust/src/my_crate.udl")
    }
}
```

### The helper plugin for linking

The helper plugin exposes two extension functions `KotlinMultiplatformExtension.hostNativeTarget`
and `KotlinNativeCompilation.useRustUpLinker`.

`hostNativeTarget` can be invoked in `kotlin {}` and adds the Kotlin Native target for the build host; it invokes
`mingwX64` on Windows, `macosX64` or `macosArm64` on macOS, and `linuxX64` or `linuxArm64` on Linux, though Linux Arm64
build host is not supported yet.

```kotlin
import io.gitlab.trixnity.gradle.rustlink.hostNativeTarget

kotlin {
    hostNativeTarget()
}
```

`useRustUpLinker` is for Kotlin Native projects referencing a Rust library but not directly using Rust. Since Kotlin
Native is shipped with an LLVM older than the one shipped with the Rust toolchain, you may encounter a linker error
when building that Kotlin Native project. `useRustUpLinker` automatically finds the LLVM linker distributed
with `rustup`, so you can use this when your Rust project emits a linker flag that is not supported by the Kotlin Native
LLVM linker.

```kotlin
import io.gitlab.trixnity.gradle.rustlink.useRustUpLinker

kotlin {
    iosArm64().compilations.getByName("main") {
        useRustUpLinker()
    }
}
```

## Directly using the bindgen CLI

Minimum Rust version required to install `uniffi_bindgen_kotlin_multiplatform` is `1.72`.
Newer Rust versions should also work fine.

Install the bindgen:

```shell
cargo install --bin uniffi-bindgen-kotlin-multiplatform uniffi_bindgen_kotlin_multiplatform@0.1.0
```

Invoke the bindgen:

```shell
uniffi-bindgen-kotlin-multiplatform --lib-file <path-to-library-file> --out-dir <output-directory> --crate <crate-name> <path-to-udl-file>
```

# Versioning

`uniffi_bindgen_kotlin_multiplatform` is versioned separately from `uniffi-rs`. UniFFI follows the
[SemVer rules from the Cargo Book](https://doc.rust-lang.org/cargo/reference/resolver.html#semver-compatibility)
which states "Versions are considered compatible if their left-most non-zero major/minor/patch
component is the same". A breaking change is any modification to the Kotlin Multiplatform bindings
that demands the consumer of the bindings to make corresponding changes to their code to ensure that
the bindings continue to function properly. `uniffi_bindgen_kotlin_multiplatform` is young, and it's
unclear how stable the generated bindings are going to be between versions. For this reason, major
version is currently 0, and most changes are probably going to bump minor version.

To ensure consistent feature set across external binding generators, `uniffi_bindgen_kotlin_multiplatform`
targets a specific `uniffi-rs` version. A consumer using these bindings or any other external
bindings (for example, [Go bindings](https://github.com/NordSecurity/uniffi-bindgen-go/) or
[C# bindings](https://github.com/NordSecurity/uniffi-bindgen-cs)) expects the same features to be
available across multiple bindings generators. This means that the consumer should choose external
binding generator versions such that each generator targets the same `uniffi-rs` version.

Here is how `uniffi_bindgen_kotlin_multiplatform` versions are tied to `uniffi-rs` are tied:

| uniffi_bindgen_kotlin_multiplatform version | uniffi-rs version |
|---------------------------------------------|-------------------|
| v0.1.0                                      | v0.25.2           |

# Build and use locally

If you want to work on the bindgen or the Gradle plugin locally,
you will have to do some additional Gradle configuration
in order to use these local versions in your projects.

## Option 1 - Dynamically include this plugin in your project

Clone this repository and reference it from your project. Configure `dependencySubstitution` to use the local plugin
version.

```kotlin
// settings.gradle.kts
pluginManagement {
    // ..
    includeBuild("../uniffi-kotlin-multiplatform-bindings/build-logic")
    // ...
    plugins {
        // comment out id("io.gitlab.trixnity.uniffi.kotlin.multiplatform") if you have it here
    }
}
// ...
includeBuild("../uniffi-kotlin-multiplatform-bindings/build-logic") {
    dependencySubstitution {
        substitute(module("io.gitlab.trixnity.uniffi.kotlin.multiplatform:gradle-plugin"))
            .using(project(":gradle-plugin"))
    }
}
```

Add the Gradle plugin to the Gradle build file.

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform")
    // ...
}
```

Optionally, configure the `uniffi` extension with the exact path to the bindgen of this repository.

```kotlin
uniffi {
    // ...
    bindgenFromPath("<path-to-our-bindgen>")
}
```

## Option 2 - Publish the plugin locally

Clone the repository and build it.

Then invoke `./gradlew :build-logic:gradle-plugin:publishToMavenLocal`.

Add the local repository in your project's `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        // ...
    }
}
```

Optionally, configure the `uniffi` extension with the exact path to the bindgen of this repository.

```kotlin
uniffi {
    // ...
    bindgenFromPath("<path-to-our-bindgen>")
}
```

You can also install the bindgen from a git remote as well. Use this method if you don't want to keep the source code of
this repository on your computer.

```kotlin
uniffi {
    bindgenFromGitTag("https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings", "v0.1.0")
}
```