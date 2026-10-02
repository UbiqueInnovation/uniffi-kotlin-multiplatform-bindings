# Runtime

Source: `runtime/`. Published as `ch.ubique.uniffi:runtime`, with the same version as the plugin.

The runtime holds everything the generated code needs that doesn't depend on a particular crate.
Generated files start with `import uniffi.runtime.*`.

## Why a separate library

Upstream UniFFI generators copy all helper code into every generated file. That works for one
crate per app, but not for several modules that share types. With a shared runtime:

- every module uses the **same** `RustBuffer`, `FfiConverter`, `UniffiHandleMap` and
  `InternalException` classes, so values can pass between modules,
- the helper code exists once per app, not once per module.

This is the basis of [multi-module support](external-and-remote-types.md#multi-module-builds).

## Contents

| Source set | Contents |
| --- | --- |
| `commonMain` | `UniffiHandleMap`, `InternalException`, `expect` declarations for `Pointer` |
| `jvmMain`, `androidMain`, `nativeMain` | `FfiConverter`, `ByteBuffer`, `RustBuffer` and `RustBufferHelper`, `ForeignBytes` / `withForeignBytes`, `uniffiRustCall`, converters for every primitive type, `String`, `ByteArray`, `Instant`, `Duration`, the async helpers, `FfiConverterCallbackInterface`, `UniffiCleaner` |
| `src/commonMain/rust/lib.rs` | a Rust crate `uniffi_runtime` that exports nothing but `setup_scaffolding!()` |

The three platform source sets are **separate hand-written copies**. JVM and Android are nearly
identical. Native differs where JNA and cinterop differ: structures, pointers, callbacks
(`staticCFunction`) and the cleaner. A fix in one usually has to be applied to all three.

## The runtime's own Rust library

The runtime is a UniFFI crate itself, with an empty interface. `setup_scaffolding!()` still exports
the standard FFI functions, in particular `ffi_uniffi_runtime_rustbuffer_alloc` and `_free`. The
module applies this repository's plugin with `bindgenFromPath(..., features = listOf("runtime"))`,
and `addRuntime = false`. The `runtime` feature makes the bindgen emit only the `UniffiLib`
declarations for this crate (see [Bindgen](bindgen.md#the-runtime-feature)). The Rust library
is packaged like any other: JVM resources, Android `jniLibs`, cinterop.

`RustBufferHelper.allocValue` calls `ffi_uniffi_runtime_rustbuffer_alloc`. **Every `RustBuffer`
that Kotlin lowers is allocated by the runtime's library**, and freed by whichever crate receives
it. This only works because all of them use the same allocator, which is true for Rust's default
system allocator. A crate that installs its own `#[global_allocator]` would break it.

## Relation to the templates

Much of the runtime started out as the templates in `bindgen/src/templates/generic/ffi/`. Some of
those templates are still used for generated code, others are dead:

- `Async.kt`, `FfiConverterTemplate.kt` and `RustBufferTemplate.kt` in `generic/ffi/` are not
  included by any template. The runtime versions are the live ones.
- The primitive converters (`Int8Helper.kt` … `StringHelper.kt`, `ByteArrayHelper.kt`,
  `TimestampHelper.kt`, `DurationHelper.kt`) are included by `generic/native/Types.kt`. Native
  bindings therefore contain their own copies of these converters, while JVM and Android bindings
  use the runtime's.

When you change behaviour that exists in both places, change both, or better, remove the duplicate.

## Tests

`runtime/src/commonTest`, `androidHostTest` and `androidDeviceTest` test the runtime directly.
`tests/runtime` is a separate fixture that uses the runtime through generated bindings.
