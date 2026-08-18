# UniFFI Kotlin Multiplatform Bindings

<p align="center">
  <a href="https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/actions/workflows/run-tests.yml">
    <img alt="Run all tests" src="https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/actions/workflows/run-tests.yml/badge.svg?branch=main" />
  </a>
  <a href="https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/actions/workflows/publish.yml">
    <img alt="Publish to Maven Central" src="https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/actions/workflows/publish.yml/badge.svg" />
  </a>
  <a href="https://central.sonatype.com/artifact/ch.ubique.uniffi/runtime">
    <img alt="Maven Central Version" src="https://img.shields.io/maven-central/v/ch.ubique.uniffi/runtime" />
  </a>
</p>

Kotlin Multiplatform binding generator for Rust libraries using Mozilla's [UniFFI](https://github.com/mozilla/uniffi-rs).

## Quickstart

Start of by creating a new rust library and adding the `uniffi` dependency:

```toml
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

Then, create a `src/commonMain/rust/lib.rs` file with the following content:

```rust
#[uniffi::export]
pub fn add(a: i32, b: i32) -> i32 {
    a + b
}

uniffi::setup_scaffolding!();
```

Next, add the gradle plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("ch.ubique.uniffi.plugin") version "1.0.0"
}
```

Finally, configure the plugin to point to your rust library:

```kotlin
uniffi {
    generateFromLibrary()
}
```

If you want your bindings to be generated with a specific package name, you can specify it in the `uniffi.toml` file:

```toml
package_name = "com.example.quickstart"
```

If you build for any Kotlin/Native target, c-interop commonization has to be enabled in your `gradle.properties`. The generated bindings live in the shared `nativeMain` source set and reference the c-interop declarations, which are only visible from a shared source set once the commonizer has run:

```properties
kotlin.mpp.enableCInteropCommonization=true
```

To see the complete example, check out the [quickstart example](examples/quickstart). For more advanced configuration options, see the [Advanced Configuration](#advanced-configuration) section below.

## Requirements

| Requirement | Version    |
| ----------- | ---------- |
| Rust        | `>=1.97.1` |
| UniFFI      | `=0.32.0`  |
| Gradle      | `>=9.6.1`  |
| Kotlin      | `>=2.4.0`  |
| AGP         | `9.x`      |

`AGP` is only required if you build for Android, see [Android](#android). The project is built and tested against AGP `9.3.1`.

## Android

Android support is built on the Android Kotlin Multiplatform library plugin, so apply `com.android.kotlin.multiplatform.library` next to the Kotlin Multiplatform plugin and declare the Android target inside the `kotlin { }` block:

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library") version "9.3.1"
    id("ch.ubique.uniffi.plugin") version "1.0.0"
}

kotlin {
    android {
        namespace = "com.example.quickstart"
        compileSdk = 36
        minSdk = 21
    }
}
```

> **Migrating from `1.0.x`:** the old setup used `com.android.library` together with `androidTarget { }` and a top level `android { }` block. Both are replaced by the above. Note that the Android configuration now lives *inside* `kotlin { }`, and that `minSdk` / `compileSdk` are set directly on it instead of in a `defaultConfig { }` block.

If the NDK version picked up by default does not work for you, pin it explicitly, see [NDK version](#ndk-version).

## Status

This project provides a Gradle plugin and a binding generator for Rust libraries using UniFFI. This project is production-ready, but might be still a bit rough around the edges. If you encounter any issues, please report them in the [issue tracker](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/issues). Currently `uniffi-rs` version `0.28.3` is supported, but support for newer versions is on the roadmap. See the [HEIDI SDK](https://github.com/heidiverse/heidi-sdk) for an example of this project in production.

If you're coming from [Uniffi Kotlin Multiplatform Bindings by Trixnity](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings), then by now a lot has changed. The `ch.ubique.uniffi.plugin` replaces the Cargo plugin (`io.gitlab.trixnity.cargo.kotlin.multiplatform`), UniFFI plugin (`io.gitlab.trixnity.uniffi.kotlin.multiplatform`), and Rust plugin (`io.gitlab.trixnity.rust.kotlin.multiplatform`). The new plugin provides a unified DSL for all configuration much faster configuration and build logic execution. If you're looking for and upgrade to the trixnity plugin, check out the `v0.7.0` release, which was the last release before the rewrite.

Alternatively, [Gobley](https://github.com/gobley/gobley) is another fork which is more similar to the original trixnity plugin, and might support newer versions of `uniffi-rs`. What sets this project apart is the [Multi Module Support](#multi-module-support) feature, which allows you to write modular and composable rust code and bindings.

## Features

### Multi Module Support

Multi Module Support allows you to write modular and composable rust code and bindings. This means you can create a set of modules, each with their own rust code and bindings, and then freely share objects between them without additional memory overhead. There is no special configuration required to use this feature, just create multiple modules, apply the plugin to each of them, and make sure to add the `Uniffi Runtime` as a dependency to each module. For more information on how to use this feature, check out the [multi-module test](tests/uniffi/multi-module/).

### External Types

[External Types](https://mozilla.github.io/uniffi-rs/0.32/types/remote_ext_types.html) are supported, but they are adviced against in favor of the multi module support.

The only case where external types are needed is if your rust library depends on a third-party rust library that also uses UniFFI. In this case, you need to generate bindings for both your rust library and the third-party rust library. To enable this, you need to set the `generateBindingsForExternalCrates` option to `true` in your `build.gradle.kts`:

```kotlin
uniffi {
    generateBindingsForExternalCrates = true
}
```

If you control the third-party rust library (for example it's a common utility library that you maintain), then it's recommended to generate the bindings for the utility library as a separate module and add the generated KMP library as a dependency. For an example, see the [multi-module test](tests/uniffi/multi-module/). Note that this approach will enable users of your library to freely choose which module they want to use.

## Advanced Configuration

### Code formatting via `ktlint`

To enable automatic code formatting of the generated bindings, you set the following option in your `build.gradle.kts`:

```kotlin
uniffi {
	formatCode = true
}
```

An installation of `ktlint` is required for this to work, it is invoked as `ktlint --format` and has to be in `PATH`. Formatting issues `ktlint` cannot fix by itself are reported as a build warning and do not fail the build.

### NDK version

The rust code for the Android targets is compiled with the NDK toolchain. By default the plugin picks the newest NDK installed under `$ANDROID_HOME/ndk`, falling back to `$ANDROID_NDK_ROOT`. To pin a specific version instead:

```kotlin
cargo {
    ndkVersion = "28.1.13356709"
}
```

### Manual dependency management

By default, the plugin will automatically manage the dependencies for the generated bindings, which means that it will add the necessary dependencies to your project. If you want to manage the dependencies yourself, for example if you want to use a different version of one of the dependencies, you can disable the automatic dependency insertion:

```kotlin
uniffi {
    addDependencies = false
}
```

Per default, these dependencies are added to `commonMain`:

| Dependency                                    | Version |
|-----------------------------------------------|---------|
| com.squareup.okio:okio                        | 3.18.1  |
| org.jetbrains.kotlinx:atomicfu                | 0.33.0  |
| org.jetbrains.kotlinx:kotlinx-coroutines-core | 1.11.0  |

In addition, `net.java.dev.jna:jna` `5.19.1` is added to the `jvmMain`, `androidMain` (as `@aar`) and `androidHostTest` source sets, since the JVM and Android bindings call into the rust library through JNA.

### Manual runtime management

To make the multi-module support work, the `Uniffi Runtime` is required as a dependency. By default, the plugin will automatically add the `Uniffi Runtime` as a dependency to your project, but you can disable this if you want to manage the runtime yourself:

```kotlin
uniffi {
    addRuntime = false
}
```

### Generating bindings for external types

If need to use the External Types feature, you need to set the following option in your `build.gradle.kts`:

```kotlin
uniffi {
    generateBindingsForExternalCrates = true
}
```

For more information on how to use this feature, check out the [External Types](#external-types) section in the README.

### Using `spmForKmp` alongside this plugin

Using [spmForKmp](https://github.com/frankois944/spm4Kmp) is supported and lets you call Swift code from Kotlin. The two plugins work together, but currently it needs a workaround due to [an issue](https://github.com/frankois944/spm4Kmp/issues/326) in how the spmForKmp plugin configures its cinterop tasks. See the [swift interop example](examples/swift-interop) for a working configuration.
