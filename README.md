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
uniffi = "0.28.3"
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

To see the complete example, check out the [quickstart example](examples/quickstart). For more advanced configuration options, see the [Advanced Configuration](#advanced-configuration) section below.

## Requirements

| Requirement | Version    |
| ----------- | ---------- |
| Rust        | `>=1.82.0` |
| UniFFI      | `=0.28.3`  |

## Status

This project provides a Gradle plugin and a binding generator for Rust libraries using UniFFI. This project is production-ready, but might be still a bit rough around the edges. If you encounter any issues, please report them in the [issue tracker](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/issues). Currently `uniffi-rs` version `0.28.3` is supported, but support for newer versions is on the roadmap. See the [HEIDI SDK](https://github.com/heidiverse/heidi-sdk) for an example of this project in production.

If you're coming from [Uniffi Kotlin Multiplatform Bindings by Trixnity](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings), then by now a lot has changed. The `ch.ubique.uniffi.plugin` replaces the Cargo plugin (`io.gitlab.trixnity.cargo.kotlin.multiplatform`), UniFFI plugin (`io.gitlab.trixnity.uniffi.kotlin.multiplatform`), and Rust plugin (`io.gitlab.trixnity.rust.kotlin.multiplatform`). The new plugin provides a unified DSL for all configuration much faster configuration and build logic execution. If you're looking for and upgrade to the trixnity plugin, check out the `v0.7.0` release, which was the last release before the rewrite.

Alternatively, [Gobley](https://github.com/gobley/gobley) is another fork which is more similar to the original trixnity plugin, and might support newer versions of `uniffi-rs`. What sets this project apart is the [Multi Module Support](#multi-module-support) feature, which allows you to write modular and composable rust code and bindings.

## Features

### Multi Module Support

Multi Module Support allows you to write modular and composable rust code and bindings. This means you can create a set of modules, each with their own rust code and bindings, and then freely share objects between them without additional memory overhead. There is no special configuration required to use this feature, just create multiple modules, apply the plugin to each of them, and make sure to add the `Uniffi Runtime` as a dependency to each module. For more information on how to use this feature, check out the [multi-module test](tests/uniffi/multi-module/).

### External Types

[External Types](https://mozilla.github.io/uniffi-rs/0.28/udl/ext_types.html) are supported, but they are adviced against in favor of the multi module support.

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

An installation of `ktlint` is required for this to work.

### Manual dependency management

By default, the plugin will automatically manage the dependencies for the generated bindings, which means that it will add the necessary dependencies to your project. If you want to manage the dependencies yourself, for example if you want to use a different version of one of the dependencies, you can disable the automatic dependency insertion:

```kotlin
uniffi {
    addDependencies = false
}
```

Per default, these dependencies are added:

| Dependency                                    | Version |
| --------------------------------------------- | ------- |
| com.squareup.okio:okio                        | 3.9.1   |
| org.jetbrains.kotlinx:atomicfu                | 0.25.0  |
| org.jetbrains.kotlinx:kotlinx-coroutines-core | 1.9.0   |
| org.jetbrains.kotlinx:kotlinx-datetime        | 0.7.1   |

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
