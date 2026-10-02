# Comparison with other projects

## Gobley

[Gobley](https://github.com/gobley/gobley) and this project try to achieve much the same thing:
Kotlin Multiplatform bindings for Rust libraries with UniFFI, built by a Gradle plugin. Both
started as forks of
[Trixnity's bindings](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings), and have
gone in different directions since. Gobley kept the structure of the original project. This
project rewrote the Gradle plugin, and keeps the bindgen up to date with
[Mozilla's UniFFI](https://github.com/mozilla/uniffi-rs).

### Multi-module support

The main difference is that this project supports [multi-module projects](features/multi-module.md):
every crate gets its own Gradle module and its own library, and any number of modules can use the
types of a shared module. Shared types are the same Kotlin classes everywhere and pass between
modules without conversion. This is possible because all code that doesn't depend on a crate,
including the allocation of `RustBuffer`s, lives in a shared [runtime](../internals/runtime.md).

Gobley only supports [external types](features/external-types.md). Each generated package has its
own copy of the helper code and is bound to one library. To use a shared crate's types from another
module, the shared module doesn't ship its own library (`embedRustLibrary = false`), and its
bindings are pointed at the dependent library instead. That covers one crate using another, but
not several modules depending on the same shared module.

### Versions

This project currently supports newer versions: UniFFI `0.32.0` (Gobley: `0.29`) and AGP 9 with
the Android Kotlin Multiplatform library plugin (Gobley: AGP 8).

### Targets

Gobley supports more targets: tvOS, watchOS, Android Native, macOS x64 and Windows MSVC for the JVM,
and it generates stubs for JS and Wasm. See [Targets](targets.md) for the targets this project
supports. Other targets can be added as well. If you need one, please
[open an issue](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/issues).

### Which one to use

Use Gobley if you need compatibility with the original Trixnity setup. Otherwise, and in particular
if you want to split your Rust code into several modules, use this project.
