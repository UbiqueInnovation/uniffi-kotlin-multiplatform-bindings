# UniFFI Kotlin Multiplatform bindings

This project contains Kotlin Multiplatform bindings generation for [UniFFI](https://github.com/mozilla/uniffi-rs).

Currently only the Kotlin targets JVM and Native are supported.
JS support would be awesome, but needs WASM support within uniffi.

You can find examples on how to use the bindings in the [tests](./tests) directory.

# How to use

> As neither the bindgen crate nor the Gradle plugin are published yet,
> you have to do some additional Gradle configuration.
> (See below [Use locally](#use-locally))

## Using the Gradle plugin

```kotlin
plugins {
    kotlin("multiplatform")
    id("net.folivo.uniffi.kotlin.multiplatform") version "0.1.0"
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

TODO

# Use locally

As neither the bindgen crate nor the Gradle plugin are published yet,
you have to do some additional Gradle configuration.

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
