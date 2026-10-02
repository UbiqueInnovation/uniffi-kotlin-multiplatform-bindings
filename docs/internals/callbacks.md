# Callbacks

This page covers how Rust calls Kotlin code: callback interfaces and `with_foreign` trait interfaces
implemented in Kotlin. The user-facing side is in
[Callbacks and trait interfaces](../guide/features/callbacks.md).

## The pieces

| Piece | Where | Does |
| --- | --- | --- |
| Handle map | `FfiConverterType<Name>.handleMap` | Keeps Kotlin implementations alive, keyed by odd `Long` handles |
| Vtable | `uniffiCallbackInterface<Name>.vtable` | A C struct of function pointers that Rust calls |
| Registration | `uniffiCallbackInterface<Name>.register(lib)` | Passes the vtable to Rust once, at library load |

Rust never sees a Kotlin object. It gets a handle, and calls a vtable function with that handle
as the first argument. The vtable function looks the object up in the handle map and calls it.

## The vtable

Generated per interface by `generic/{android+jvm,native}/CallbackInterfaceImpl.kt`:

```kotlin
internal object uniffiCallbackInterfaceLogger {
    internal object log : UniffiCallbackInterfaceLoggerMethod0 {
        override fun callback(uniffiHandle: Long, message: RustBufferByValue,
                              uniffiOutReturn: Pointer, uniffiCallStatus: UniffiRustCallStatus) {
            val uniffiObj = FfiConverterTypeLogger.handleMap.get(uniffiHandle)
            val makeCall = { -> uniffiObj.log(FfiConverterString.lift(message)) }
            val writeReturn = { _: Unit -> Unit }
            uniffiTraitInterfaceCall(uniffiCallStatus, makeCall, writeReturn)
        }
    }
    internal object uniffiFree : UniffiCallbackInterfaceFree {
        override fun callback(handle: Long) { FfiConverterTypeLogger.handleMap.remove(handle) }
    }
    internal object uniffiClone : UniffiCallbackInterfaceClone {
        override fun callback(handle: Long): Long = FfiConverterTypeLogger.handleMap.clone(handle)
    }

    internal val vtable = UniffiVTableCallbackInterfaceLogger(uniffiFree, uniffiClone, log)

    internal fun register(lib: UniffiLib) {
        lib.uniffi_my_crate_fn_init_callback_vtable_logger(vtable)
    }
}
```

- **Field order is part of the ABI**: `free`, `clone`, then the methods in declaration order. It
  must match the struct UniFFI's scaffolding expects. UniFFI changed this order in 0.30.
- **Results and errors go through out-parameters**, never the C return value. The trampoline
  writes the lowered result to `uniffiOutReturn` and sets `uniffiCallStatus`.
- **Exceptions**: `uniffiTraitInterfaceCall` / `…WithError` catch exceptions. An exception of the
  declared error type is lowered into the status' error buffer (`CALL_ERROR`). Any other exception
  becomes `CALL_UNEXPECTED_ERROR` with `toString()` as message. Rust turns that into the error
  type via `From<UnexpectedUniFFICallbackError>`, or panics if the method has no error type.

Per platform:

| | JVM / Android | Native |
| --- | --- | --- |
| vtable entry | `internal object` implementing a JNA `Callback` | `staticCFunction { … }` |
| vtable struct | JNA `Structure` | `nativeHeap.alloc<cinterop.UniffiVTable…>()` |

Neither is ever freed. Rust may call the vtable at any point for the rest of the process, and JNA
doesn't keep callbacks reachable by itself, so the singletons must stay alive.
`staticCFunction` can't capture variables, which is why everything is looked up through global
objects and handle maps.

## Registration

`CodeType::initialization_fn()` returns `uniffiCallbackInterface<Name>.register` for every callback
interface and every trait with a foreign implementation (`object.rs`, `callback_interface.rs`).
`initialization_fns()` in `mod.rs` collects them, and the `UniffiLib.INSTANCE` initialiser calls
them when the library is first used:

```kotlin
internal val INSTANCE: UniffiLib by lazy {
    loadIndirect<UniffiLib>(componentName = "my_crate").also { lib ->
        uniffiCheckContractApiVersion(lib)
        uniffiCheckApiChecksums(lib)
        uniffiCallbackInterfaceLogger.register(lib)    // our own vtables
        other_crate.uniffiEnsureInitialized()          // crates whose types we use
    }
}
```

The `uniffiEnsureInitialized()` calls make sure that another crate's vtables are registered before
our crate could pass one of its traits to Rust. This is the fix for
[uniffi-rs#2343](https://github.com/mozilla/uniffi-rs/issues/2343). Without it, Rust could call
through an unset vtable, which aborts the process.

## Ownership

Every `lower` of a Kotlin implementation inserts a **new** entry into the handle map. Passing the
same object twice gives two independent handles, each released separately by Rust.

| Event | Handle map |
| --- | --- |
| Kotlin passes an implementation to Rust (`lower`) | `insert`, new odd handle |
| Rust clones its `Arc<dyn Trait>` | vtable `clone` → `clone(handle)`, another new handle |
| Rust drops it | vtable `free` → `remove(handle)` |
| Rust returns a `with_foreign` object to Kotlin (`lift`) | `remove(handle)`, ownership ends here |
| Rust returns a plain callback interface to Kotlin (`lift`) | `get(handle)`, Rust still owns it |

Plain callback interfaces use the runtime's `FfiConverterCallbackInterface`, where `lift` is `get`.
Trait interfaces use the generated object converter, where `lift` is `remove` (see
[Objects and handles](objects-and-handles.md#trait-interfaces)). Mixing these up is either a leak
or a use-after-free, and only shows up later as `InternalException: UniffiHandleMap: Invalid handle`.

## Multi-module limitation

The vtable lives in the scaffolding of the crate that **declares** the trait, in a static that is
filled by `register`. In a [multi-module build](external-and-remote-types.md#multi-module-builds)
each module's library contains its own copy of that static. Kotlin only registers the vtable with
the declaring crate's own library, so a Kotlin implementation passed to a function of another
module's library hits an empty vtable. The `testCallback` case in
`tests/uniffi/multi-module/mod-a/.../ModATest.kt` is commented out for this reason.

## Async methods

Async callback methods return a foreign future instead of a value. See
[Async](async.md#kotlin-suspend-rust-future).
