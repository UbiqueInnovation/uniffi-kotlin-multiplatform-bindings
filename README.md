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

Here is an example of using the Gradle plugin to build bindings for a crate in a `rust` subdirectory.

```kotlin
plugins {
    kotlin("multiplatform")
    id("io.gitlab.trixnity.uniffi.kotlin.multiplatform") version "0.1.0"
}

uniffi {
    // The directory containing the Rust crate.
    crateDirectory = layout.projectDirectory.dir("rust")

    // The name of the crate as in Cargo.toml's package.name.
    crateName = "my_crate"

    // The name of the library as in Cargo.toml's library.name. Defaults to "${crateName}".
    libraryName = "my_crate"

    // The UDL file. Defaults to "${crateDirectory}/src/${crateName}.udl".
    udlFile = layout.projectDirectory.file("rust/src/my_crate.udl")

    // The UDL namespace as in the UDL file. Defaults to "${crateName}".
    namespace = "my_crate"

    // The profile to build the crate. Defaults to "debug".
    profile = "debug"
}

kotlin {
    // The plugin will react to the targets definition
    jvm()
    linuxX64()
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

If you want to work on the bingen or the Gradle plugin locally,
you will have to do some additional Gradle configuration
in order to use these local versions in your projects.

## Publish Gradle plugin in a local repository

Clone the repository and build it.
Add a publishing repository definition at the end of `build-logic/gradle-plugin/build.gradle.kts`:

```kotlin
publishing {
    repositories {
        maven {
            name = "local"
            url = uri("<path-to-local-plugin-repository>")
        }
    }
}
```

Then invoke `./gradlew :build-logic:gradle-plugin:publishAllPublicationsToLocalRepository`.

## Specify the local repository

Add the local repository in you `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven {
            name = "local"
            url = uri("<path-to-local-plugin-repository>")
        }
        // ...
    }
}
```

Finally, configure the `uniffi` extension with the exact path to the bindgen of this repository.

```kotlin
uniffi {
    // ...
    bindgenCratePath = "<path-to-our-bindgen>"
}
```
