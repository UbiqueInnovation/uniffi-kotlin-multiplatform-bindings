# Bindgen

Source: `bindgen/`. The crate `uniffi_bindgen_kotlin_multiplatform` builds the binary
`uniffi-bindgen-kotlin-multiplatform`.

| File | Role |
| --- | --- |
| `src/main.rs` | CLI. Library mode (`--library`) or UDL mode, `--crate`, `--out-dir`. |
| `src/lib.rs` | `KotlinBindingGenerator`, the `BindingGenerator` implementation, and writing files. |
| `src/gen_kotlin_multiplatform/mod.rs` | `Config`, the `CodeType` trait, `KotlinCodeOracle`, the template structs, askama filters. |
| `src/gen_kotlin_multiplatform/{primitives,miscellany,compounds,record,enum_,variant,object,callback_interface,custom}.rs` | One `CodeType` implementation per kind of type. |
| `src/templates/` | The askama templates. |
| `askama.toml` | Registers the `kt` and `c` template syntaxes. |

## Flow

`main.rs` calls `uniffi_bindgen::library_mode::generate_bindings` (or
`generate_external_bindings` for UDL) with our generator. UniFFI then calls:

1. `new_config`: deserialise each crate's `uniffi.toml` into `Config`.
2. `update_component_configs`: apply the `--package-name` override, if given, to the selected
   crate (`--crate`, or the crate derived from the library file name). It must happen here, before
   the package map below is built, so that other crates importing this one see the new package.
   Then default `package_name` to `uniffi.<namespace>` and `cdylib_name`
   to the library's name, then build a crate → package map over all components and add it to every
   component's `external_packages`. You will see `Adding external package mapping for crate …` in
   the build log. That is this step.
3. `write_bindings`: for each component, `generate_bindings(config, ci)` renders six strings, which
   are written to `{common,jvm,android,native}Main/kotlin/<package>/<namespace>.<target>.kt` and to
   the two headers.

## Two-pass rendering

Each Kotlin file is rendered by a pair of structs created with the `kotlin_type_renderer!` and
`kotlin_wrapper!` macros in `mod.rs`:

- The **type renderer** (`…/Types.kt`) runs first and renders the code for every type. While
  rendering, templates call `self.add_import("…")` and `self.include_once_check("…")`. These
  collect imports and track which shared helpers were emitted, in `RefCell`s.
- The **wrapper** (`…/wrapper.kt`) runs second. It gets the type renderer's output as
  `type_helper_code`, and can print the collected imports at the top of the file.

So the imports end up at the top of the file even though they are only known after the body has
been rendered. `render()` on a type renderer must only be called once.

| Struct | Template | Output |
| --- | --- | --- |
| `CommonKotlinWrapper` | `generic/common/wrapper.kt` | `commonMain` |
| `AndroidJvmKotlinWrapper` | `generic/android+jvm/wrapper.kt` | `jvmMain` and `androidMain` |
| `NativeKotlinWrapper` | `generic/native/wrapper.kt` | `nativeMain` |
| `HeaderKotlinWrapper` | `generic/headers/wrapper.h` | `headers/<ns>/<ns>.h` |
| `CommonHeaderKotlinWrapper` | `generic/headers/common.h` | `headers/common/common.h` |

## `CodeType` and the two big matches

`KotlinCodeOracle::find(&Type)` maps a UniFFI `Type` to a `Box<dyn CodeType>`. A `CodeType`
answers the Kotlin-specific questions about a type:

| Method | Gives |
| --- | --- |
| `type_label(ci)` | the Kotlin type, e.g. `List<kotlin.String>` |
| `canonical_name()` | a name fragment, e.g. `SequenceString`, used in `FfiConverterSequenceString` |
| `ffi_converter_name()` | `FfiConverter<canonical_name>` |
| `default(value, ci, config)` | Kotlin source for a default value |
| `initialization_fn()` | a function to call when the library is loaded, used for callback vtables |

Each `Types.kt` template has a matching `match type_` that includes the right template per type.
When you add a type, **both** matches have to change, `KotlinCodeOracle::find` and the template
matches. The comments at both places say so.

Templates reach the oracle through askama filters in `mod.rs` (`filters` module): `type_name`,
`ffi_converter_name`, `lower_fn`, `lift_fn`, `read_fn`, `write_fn`, `class_name`, `fn_name`,
`var_name`, `ffi_type_name*`, `render_default`, `async_poll` / `async_complete` / `async_free` /
`async_cancel`, `docstring`, and so on. Naming rules (`UpperCamelCase` classes,
`lowerCamelCase` functions, `Error` → `Exception`, keyword escaping) are methods on
`KotlinCodeOracle`.

## Templates

```
bindgen/src/templates/
├── macros.kt              shared macros: function bodies, FFI calls, argument lists, docstrings
├── generic/               normal builds
│   ├── common/            commonMain: public API, expect declarations
│   ├── android+jvm/       JVM/Android: actuals, JNA library and structures
│   ├── native/            Native: actuals, cinterop typealiases
│   ├── ffi/               shared by android+jvm and native: converters for records, enums,
│   │                      collections, custom types, object bodies
│   └── headers/           C headers for cinterop
└── runtime/               builds with the `runtime` feature, used only by the runtime module
```

`generic/ffi/` is included from both `android+jvm/Types.kt` and `native/Types.kt`, so most
conversion code is written once and compiled for both platforms. These templates can only use
API that exists on both, and rely on the [runtime](runtime.md) for anything that differs
(`ByteBuffer`, `RustBuffer`, `Pointer`, the cleaner). Converters that don't depend on the crate,
like the ones for primitives, `String` and `Duration`, are not generated at all; they come from the
runtime.

`macros.kt` produces the bodies of all calls. The important macros:

| Macro | Does |
| --- | --- |
| `func_decl_with_body` | a complete function or method, sync or `suspend` |
| `to_ffi_call` | wraps the call in `callWithHandle` when the receiver is an object |
| `to_raw_ffi_call` | `uniffiRustCall` / `uniffiRustCallWithError`, and `withForeignBytes` scopes for `&[u8]` arguments |
| `call_async` | the `uniffiRustCallAsync(...)` call for `async` callables |
| `self_shim_*`, `self_method_decl`, `self_uniffi_trait_impls` | methods and trait members on records and enums |

### The `runtime` feature

With `--features runtime`, the Kotlin templates point into `templates/runtime/` instead (see the
`#[cfg(feature = "runtime")]` blocks in `mod.rs`). Those templates only emit the package
declaration and the `UniffiLib` glue for the runtime crate itself. The headers come from the same
templates as for any other crate. `runtime/build.gradle.kts`
installs the bindgen with that feature to generate the FFI declarations it needs, while the
runtime's converters and helpers are written by hand. See [Runtime](runtime.md).

## Headers

Kotlin/Native reads the FFI declarations from C headers through cinterop. Two are written per
crate:

| File | Contents |
| --- | --- |
| `headers/<ns>/<ns>.h` | this namespace's functions, structs and callback types, and a `typedef RustBuffer RustBuffer<Name>` per external type |
| `headers/common/common.h` | `RustBuffer`, `ForeignBytes`, `RustCallStatus`, and every name in `FFI_BUILTINS` |

`FFI_BUILTINS` in `mod.rs` lists the FFI definitions that are identical for every crate:
future continuation callbacks, foreign-future result structs, the vtable `free` / `clone` callbacks.
They go into `common.h` once, rather than into each namespace header. When one module generates
several namespaces (`generateBindingsForExternalCrates`), all headers end up in the same cinterop,
and a definition that appears in two namespace headers breaks it. If UniFFI renames or adds such a
definition, update the list.

`common.h` is the same for every crate, and the runtime's cinterop klib ships it too. cinterop
matches headers by content, so a crate's klib reuses the runtime's `RustBuffer`,
`UniffiRustCallStatus` and builtins instead of declaring its own. Otherwise every klib declares
`cinterop.RustBuffer` again, and linking with Kotlin 2.4.20 fails with
`IrClassSymbolImpl is already bound`
([#29](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/issues/29)).
**Keep `common.h` independent of the crate.**

JVM and Android don't use the headers. They declare the same structures as JNA classes in
`generic/android+jvm/NamespaceLibraryTemplate.kt`. A struct that changes therefore needs to change
in both places. A mistake there typically compiles on JVM and only fails on Native, or the other way
round. **Always build a native target after touching anything struct-shaped.**

## Where to change what

| To change… | Edit |
| --- | --- |
| the public Kotlin API of a type | `templates/generic/common/<X>Template.kt`, and the matching `actual` in `generic/ffi/<X>Template.kt` |
| how a type is converted | `templates/generic/ffi/<X>Template.kt` |
| a method or function body | `templates/macros.kt` |
| FFI declarations on JVM/Android | `templates/generic/android+jvm/NamespaceLibraryTemplate.kt` |
| FFI declarations on Native | `templates/generic/native/NamespaceLibraryTemplate.kt` and `templates/generic/headers/` |
| a naming rule | `KotlinCodeOracle` in `mod.rs` |
| a `uniffi.toml` option | `Config` in `mod.rs` |
| a helper available in templates | the `filters` module in `mod.rs` |
| code that doesn't depend on the crate | `runtime/src/{jvmMain,androidMain,nativeMain}/` |
