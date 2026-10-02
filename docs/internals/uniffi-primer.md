# UniFFI primer

This page covers the parts of UniFFI the rest of the code depends on. UniFFI's own
[internals documentation](https://mozilla.github.io/uniffi-rs/latest/internals/design_principles.html)
goes deeper.

UniFFI has two halves:

- **Scaffolding** (Rust side): the `#[uniffi::export]` and `derive` macros, or `build.rs` for UDL,
  generate `extern "C"` functions that wrap your Rust API. They live in your crate, inside the
  compiled library.
- **Bindings** (foreign side): a *binding generator* reads a description of the interface and
  writes code that calls those `extern "C"` functions. UniFFI ships generators for Kotlin
  (JVM and Android only), Swift, Python and Ruby. This repository's bindgen is one for Kotlin Multiplatform.
  It started as a fork of the upstream Kotlin generator, and a lot of the template code still
  mirrors it.

Both halves must agree exactly on the FFI, which is why the UniFFI version is pinned.

## The interface description

`uniffi_bindgen::ComponentInterface` (usually `ci` in code) is UniFFI's model of **one crate's**
exported API: functions, objects, records, enums, errors, callback interfaces, and the FFI
functions behind them. Templates query it directly, for example `ci.function_definitions()`,
`ci.iter_local_types()`, `ci.iter_ffi_function_definitions()`.

Where it comes from:

- **Library mode** (`generateFromLibrary`, `--library`): the scaffolding macros embed metadata
  symbols in the compiled library. `uniffi_bindgen::library_mode::generate_bindings` reads them
  back and builds one `ComponentInterface` per UniFFI crate linked into the library. It reads each
  crate's `uniffi.toml` through `cargo metadata`.
- **UDL mode**: the UDL file is parsed directly.

Library mode sees dependencies too. That is what makes external types and multi-module builds
possible.

## The generator interface

A binding generator implements `uniffi_bindgen::BindingGenerator`:

| Method | Here (`bindgen/src/lib.rs`) |
| --- | --- |
| `new_config(toml)` | Deserialises `uniffi.toml` into our `Config`. |
| `update_component_configs(settings, components)` | Fills in defaults (`package_name`, `cdylib_name`) and maps every crate to its Kotlin package (`external_packages`). Runs once with **all** components, before any rendering. |
| `write_bindings(settings, components)` | Renders and writes the files for each component. |

## Lowering and lifting

Every type that crosses the FFI has an **FFI type**, `uniffi_bindgen::interface::FfiType`, which is
the C-level representation:

| FfiType | C | Used for |
| --- | --- | --- |
| `Int8` … `UInt64`, `Float32`, `Float64` | integer / float | primitives, `bool` (as `Int8`) |
| `Handle` | `uint64_t` | objects, callback interfaces, futures |
| `RustBuffer` | struct `{ capacity, len, data }` | strings, records, enums, collections, optionals |
| `ForeignBytes` | struct `{ len, data }` | borrowed `&[u8]` arguments |
| `RustCallStatus` | struct `{ code, error_buf }` | error reporting, as an out-parameter |
| `Callback`, `Struct` | function pointer, struct | vtables and async plumbing |

Turning a value into its FFI type is **lowering**, turning it back is **lifting**. Compound values
are **written** into a `RustBuffer` in a simple big-endian format and **read** back. On the Kotlin
side every type has an `FfiConverter` with these operations:

```kotlin
interface FfiConverter<KotlinType, FfiType> {
    fun lift(value: FfiType): KotlinType
    fun lower(value: KotlinType): FfiType
    fun read(buf: ByteBuffer): KotlinType
    fun write(value: KotlinType, buf: ByteBuffer)
    fun allocationSize(value: KotlinType): ULong
}
```

Ownership follows the direction: **lowering hands ownership to the receiver, lifting takes it.**
Lifting a `RustBuffer` frees it. Lowering an object hands over a fresh reference. Most lifetime
rules in the generated code follow from this.

## Calls and errors

Every scaffolding function takes a `RustCallStatus*` as its last argument:

| `code` | Meaning | `error_buf` |
| --- | --- | --- |
| `0` | success | empty |
| `1` | the function returned `Err(e)` | `e`, serialised |
| `2` | Rust panicked | the panic message, or empty |

The Kotlin side checks it after every call, in `uniffiRustCall` / `uniffiRustCallWithError`.
Callbacks from Rust to Kotlin use the same struct in the other direction.

## Naming of FFI symbols

The scaffolding names follow fixed patterns, which helps when reading headers or `nm` output:

```
uniffi_<crate>_fn_func_<function>
uniffi_<crate>_fn_method_<object>_<method>
uniffi_<crate>_fn_constructor_<object>_<name>
uniffi_<crate>_fn_clone_<object>, uniffi_<crate>_fn_free_<object>
uniffi_<crate>_fn_init_callback_vtable_<trait>
uniffi_<crate>_checksum_<...>
ffi_<crate>_rustbuffer_alloc / _free / ...
ffi_<crate>_rust_future_poll_<type> / _complete_ / _free_ / _cancel_
ffi_<crate>_uniffi_contract_version
```

## Safety checks

- **Contract version**: `ffi_<crate>_uniffi_contract_version()` returns a number that changes
  whenever UniFFI's FFI conventions change. The bindings compare it with the version they were
  generated for.
- **Checksums**: `uniffi_<crate>_checksum_*` returns a hash of one function's signature. The
  bindings compare all of them at load time, unless `omit_checksums` is set.

Both checks happen in the lazy `UniffiLib.INSTANCE` initialiser, and only on JVM and Android,
where the library is loaded at runtime.
