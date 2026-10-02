# UniFFI Kotlin Multiplatform Bindings

A Gradle plugin and binding generator that turns a Rust library into a Kotlin Multiplatform
library, using Mozilla's [UniFFI](https://github.com/mozilla/uniffi-rs).

You annotate your Rust code with UniFFI's macros (or describe it in a UDL file). The plugin then
builds the crate for every Kotlin target in your project, generates Kotlin bindings for it, and
wires the bindings and native libraries into the right source sets. Your Kotlin code calls the
Rust functions from `commonMain`. The plugin handles the rest.

```rust
#[uniffi::export]
pub fn add(a: i32, b: i32) -> i32 {
    a + b
}
```

```kotlin
import com.example.quickstart.add

add(2, 2) // 4, on JVM, Android, iOS, macOS, Linux and Windows
```

## Supported targets

| Kotlin target | How Rust is called |
| --- | --- |
| `jvm()` | JNA, dynamic library bundled as a resource |
| `android { }` | JNA, dynamic library bundled as `jniLibs` |
| `iosArm64()`, `iosSimulatorArm64()`, `iosX64()`, `macosArm64()` | cinterop, static library |
| `linuxX64()`, `linuxArm64()` | cinterop, static library |
| `mingwX64()` | cinterop, static library |

## How this documentation is organised

**[User guide](guide/getting-started.md)** is for people using the plugin. It starts with a
project setup and then walks through every feature the generated bindings support, with the Rust
side, the Kotlin you get, and the configuration involved.

**[Internals](internals/index.md)** is for people working on this repository. It explains how
the Gradle plugin, the bindgen and the runtime fit together, and covers the parts of UniFFI you
need to understand to change them.

## Relation to other projects

This project started as a fork of
[UniFFI Kotlin Multiplatform Bindings by Trixnity](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings).
Since then it has been largely rewritten. A single plugin, `ch.ubique.uniffi.plugin`, replaces the
three Trixnity plugins (Cargo, UniFFI and Rust), with one DSL and much faster configuration. The
last release before the rewrite was `v0.7.0`.

[Gobley](https://github.com/gobley/gobley) is another fork, closer to the original Trixnity
plugins. What sets this project apart is [multi-module support](guide/features/multi-module.md):
you can split your Rust code into several crates, publish each as its own Kotlin library, and pass
objects between them without copying.

This project is used in production, for example in the [HEIDI SDK](https://github.com/heidiverse/heidi-sdk).
Please report problems in the
[issue tracker](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/issues).
