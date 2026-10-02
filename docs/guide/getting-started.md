# Getting started

This page builds a minimal Kotlin Multiplatform library with one Rust function. It mirrors
[`examples/quickstart`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/examples/quickstart),
which you can also use as a starting point.

This guide follows the convention used throughout this repository: the crate's `Cargo.toml` sits
next to `build.gradle.kts`, and the Rust sources go into `src/commonMain/rust`, next to the Kotlin
sources. Neither is required. Cargo's default `src/lib.rs` works just as well, and the crate can
live in a different directory (see [`packageDirectory`](configuration/gradle-dsl.md#cargo)).

```
quickstart/
├── Cargo.toml
├── uniffi.toml
├── build.gradle.kts
└── src/
    ├── commonMain/rust/lib.rs
    └── commonTest/kotlin/QuickstartTest.kt
```

## 1. The Rust crate

Create a library crate that depends on `uniffi`:

```toml
# Cargo.toml
[package]
name = "uniffi-kmm-example-quickstart"
version = "0.1.0"
edition = "2021"
publish = false

[lib]
name = "uniffi_kmm_example_quickstart"
crate-type = ["lib", "cdylib", "staticlib"]
path = "src/commonMain/rust/lib.rs"

[dependencies]
uniffi = "0.32.0"
```

The JVM and Android targets load a `cdylib`, and Kotlin/Native targets link a `staticlib`. The
plugin asks Cargo for exactly the crate type each build needs, but declaring both keeps plain
`cargo build` working. `lib` is needed if another Rust crate depends on this one, for example in a
[multi-module project](features/multi-module.md).

Export something:

```rust
// src/commonMain/rust/lib.rs
#[uniffi::export]
pub fn add(a: i32, b: i32) -> i32 {
    a + b
}

uniffi::setup_scaffolding!();
```

`setup_scaffolding!()` generates the FFI glue UniFFI needs. It must appear exactly once per crate.

## 2. The Gradle module

Apply the Kotlin Multiplatform plugin and this plugin, then tell it how to generate bindings:

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.4.0"
    id("ch.ubique.uniffi.plugin") version "1.2.1"
}

uniffi {
    generateFromLibrary()
}

kotlin {
    jvmToolchain(17)

    jvm()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    linuxX64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
```

The plugin is published to Maven Central, so make sure `mavenCentral()` is in your
`pluginManagement { repositories { } }` block in `settings.gradle.kts`.

`generateFromLibrary()` builds the crate for your machine, reads the interface UniFFI embedded in
the binary, and generates bindings from it. This works for both proc-macro and UDL crates, see
[Proc-macros and UDL](proc-macros-vs-udl.md).

You don't need to add any dependencies yourself. The plugin adds the UniFFI runtime and the
libraries the generated code uses, see [Dependencies](configuration/dependencies.md).

## 3. Enable cinterop commonization

If you have any Kotlin/Native target (iOS, macOS, Linux, Windows), add this to
`gradle.properties`:

```properties
kotlin.mpp.enableCInteropCommonization=true
```

The generated native bindings live in the shared `nativeMain` source set and use the cinterop
declarations from there. Those declarations are only visible from a shared source set once the
commonizer has run. Without this flag, the plugin fails the build with a message telling you to set it.

## 4. Choose a package name

By default the bindings go into the package `uniffi.<namespace>`, where the namespace is the
crate's library name. To choose your own, add a `uniffi.toml` next to `Cargo.toml`:

```toml
package_name = "com.example.quickstart"
```

All options are listed in [uniffi.toml](configuration/uniffi-toml.md).

## 5. Call it from Kotlin

```kotlin
// src/commonTest/kotlin/QuickstartTest.kt
import com.example.quickstart.add
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickstartTest {
    @Test
    fun itWorks() {
        assertEquals(4, add(2, 2))
    }
}
```

Run the tests on every target:

```bash
./gradlew :quickstart:allTests
```

## What happened

When Gradle compiled the Kotlin code, the plugin:

1. installed the binding generator (`installBindgen`),
2. built the crate for your host and generated Kotlin from it (`buildBindings`), writing to
   `build/uniffi/bindings/`,
3. built the crate once per Kotlin target (`cargoBuild<Target><Debug|Release>`),
4. packaged the native libraries for each target: as JVM resources, as Android `jniLibs`, or
   through cinterop for Kotlin/Native.

The generated code is plain Kotlin. Open `build/uniffi/bindings/commonMain` to see the API your
crate exposes. The first build takes a while because the generator and the Rust dependencies are
compiled. Later builds reuse Cargo's cache.

!!! note "Debug and release"
    By default everything is built in debug mode, and JVM builds only include the library for your
    host. Pass `-PreleaseBuild=true` to build optimised libraries for all platforms, see
    [Targets](targets.md#debug-and-release-builds).

## Next steps

- Configure [Android and the other targets](targets.md).
- Go through the [features](features/functions-and-objects.md) to see what you can export.
- Read the [Gradle DSL](configuration/gradle-dsl.md) reference.
