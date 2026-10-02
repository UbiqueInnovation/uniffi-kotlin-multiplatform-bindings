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

add(2, 2) // 4
```

## Supported targets

| Kotlin target                                                   | How Rust is called                         |
| --------------------------------------------------------------- | ------------------------------------------ |
| `jvm()`                                                         | JNA, dynamic library bundled as a resource |
| `android { }`                                                   | JNA, dynamic library bundled as `jniLibs`  |
| `iosArm64()`, `iosSimulatorArm64()`, `iosX64()`, `macosArm64()` | cinterop, static library                   |
| `linuxX64()`, `linuxArm64()`                                    | cinterop, static library                   |
| `mingwX64()`                                                    | cinterop, static library                   |

## Where to go from here

If you want to use the plugin, start with the [user guide](guide/getting-started.md). It sets up a
small project first, and then goes through the features one by one: what you write in Rust, the
Kotlin you get, and the configuration involved.

If you want to work on the plugin itself, read the [internals](internals/index.md). They explain
how the Gradle plugin, the binding generator and the runtime fit together, and cover the parts of
UniFFI you need to know to change them.

## Relation to other projects

This project started as a fork of
[UniFFI Kotlin Multiplatform Bindings by Trixnity](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings),
and has been largely rewritten since.

[Gobley](https://github.com/gobley/gobley) is another project with the same goal. See
[Comparison with Gobley](comparison.md) for how the two differ.

This project is used in production, for example in the [Kapun SDK](https://github.com/KapunSDK/kapun-sdk).
Please report problems in the
[issue tracker](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/issues).
