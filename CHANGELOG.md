# Changelog

## [Unreleased](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/compare/v1.1.1...HEAD)

### Added

- Borrowed bytes support (`&[u8]` in Rust, `[ByRef] bytes` in UDL, new in uniffi `0.32`). Such an argument crosses the
  FFI as a `ForeignBytes` - a pointer into the caller's buffer plus a length - instead of being copied into a
  `RustBuffer`. The Kotlin signature is an ordinary `ByteArray`, the same as for `Vec<u8>`, so which one your Rust
  takes is invisible to consumers. Kotlin/Native pins the caller's array and copies nothing; JVM and Android copy it
  into native memory for the duration of the call, since a `ByteArray` on the managed heap has no address to lend.
  Either way the borrow ends when the call returns, which is what `ForeignBytes` requires. Not supported in two
  positions, both of which the bindgen refuses with a message rather than generating: on an `async` function, where the
  borrow would end while the Rust future was still reading it, and on a callback or trait interface method, where the
  value travels Rust to Kotlin - `ForeignBytes` has no `Lower` impl, so that does not compile in Rust either.
- `HashSet` support (`Type::Set`, new in uniffi `0.32`). A `HashSet<T>` argument, return value or field is generated as
  a Kotlin `Set<T>`, and `#[uniffi(default)]` on such a field gives `setOf()`. Only reachable through the proc-macros -
  UDL has no syntax for a set.
- Methods on records and enums (new in uniffi `0.31`). An `#[uniffi::export] impl` block on a `uniffi::Record` or
  `uniffi::Enum` now reaches Kotlin as ordinary member functions on the generated `data class`, `enum class` or
  `sealed class`. Unlike an object, whose class is `expect`/`actual`, a record or enum is declared once in `commonMain`,
  where the FFI is not in scope - each method therefore delegates to an `internal expect fun uniffiSelfCall_...` shim
  whose `actual` sits next to the type's `FfiConverter` in each platform source set. The receiver crosses the FFI as a
  serialized value rather than as a handle.
- uniffi trait exports on records and enums: `Display`/`Debug`, `Eq`, `Hash` and `Ord` become `toString`, `equals`,
  `hashCode` and `compareTo` there too, the same way they already did on objects. Each variant of a `sealed class`
  repeats them, because a `data class` would otherwise shadow the base's overrides with its own generated ones. A
  fieldless enum is the exception: `kotlin.Enum` declares `equals`, `hashCode` and `compareTo` `final`, so only
  `Display`/`Debug` is rendered for a Kotlin `enum class` - the other three keep Kotlin's own behaviour, which agrees
  with Rust's derives unless the variants carry explicit out-of-order discriminants.
- `Ord` on objects (`[Traits=(Ord)]` in UDL, `#[uniffi::export(Ord)]` on an `impl`). The generated class implements
  `Comparable<T>` and gets a `compareTo` backed by the Rust `Ord` impl; only `Display`, `Eq` and `Hash` were emitted
  before. `Debug` is also honoured as a fallback for `toString` now when a type exports it without `Display`.
- `uniffiIsDestroyed` on generated objects (new in uniffi `0.32`), a read-only `Boolean` reporting whether `destroy()`
  has run and the object's handle on the Rust side is gone. The flag was already tracked internally to make `destroy()`
  idempotent; this just exposes it, so callers can ask instead of discovering it from the `IllegalStateException` the
  next method call throws.

### Changed

- **Records are generated with `val` fields by default.** `generate_immutable_records` defaults to `true` now (upstream
  defaults it to `false`), because a record is a snapshot of what crossed the FFI - assigning to a field of one only
  ever changed the Kotlin copy, never anything on the Rust side. Existing code that mutates a record field no longer
  compiles; replace the assignment with `copy(field = ...)`, or set `generate_immutable_records = false` in
  `uniffi.toml` to keep `var` everywhere, or name the individual records in `mutable_records`.
- Update uniffi-rs to `v0.32.0` (from `v0.28.3`). This is a breaking change for consumers - the crates you build with
  have to move to `uniffi = "0.32.0"` together with the plugin, and the FFI is not compatible across the two versions.
  What this means for your Rust sources:
  - Custom types no longer implement `UniffiCustomTypeConverter`. Use `uniffi::custom_type!(MyType, Builtin, { lower: ..., try_lift: ... })`
    or `uniffi::custom_newtype!(MyType, Builtin)` instead, and add `remote` to the `custom_type!` body for types from
    another crate.
  - Types described in your UDL but defined in a third-party crate need the `[Remote]` attribute, and remote custom
    types need a `uniffi::use_remote_type!(their_crate::TheType)` in your Rust source. The `uniffi::use_udl_record!`,
    `use_udl_enum!`, `use_udl_object!` and `ffi_converter_forward!` macros are gone - external types are ordinary types
    now and need no declaration.
  - Trait interfaces declared in UDL (`[Trait]` / `[Trait, WithForeign]`) need `#[uniffi::trait_interface]` on the
    Rust trait.
- Objects cross the FFI as an opaque 64-bit handle rather than a pointer. The generated cinterop headers declare
  `uint64_t` where they used to declare `void *`, and the Kotlin side carries a `Long`. Handles for trait interfaces are
  bidirectional, tagged by their lowest bit.
- The marker object for constructing an interface fake is called `NoHandle`, matching the upstream rename from
  `NoPointer` - what it marks is a handle now. It is still emitted into your binding's own package. Test fakes that
  subclass a generated interface have to be updated: `import your.pkg.NoPointer` becomes `import your.pkg.NoHandle`,
  and `class FakeThing : Thing(NoPointer)` becomes `class FakeThing : Thing(NoHandle)`.
- The callback interface vtable gained a `uniffi_clone` entry and `uniffi_free` moved from last to first. Both the
  runtime and the generated code had to move together - a binding generated by an older bindgen against a `0.32`
  scaffolding will crash rather than fail to load.
- Method checksums no longer fold in the self type, so a cached bindgen binary from before this release will fail the
  startup integrity check against `0.32` scaffolding. Let the plugin reinstall the bindgen after upgrading.
- The Rust toolchain moved to `1.97.1`.

### Fixed

- `#[uniffi(default = ...)]` on a field of an enum variant is honoured. Only record fields were rendered with their
  default; every field of a `sealed class` variant was emitted without one, so the defaults were dropped and callers
  had to pass all of them explicitly. Optional fields still got a `= null` from a separate code path, which is what
  made the gap easy to miss.
- A default value on a custom type is converted with that type's `lift` expression. It was rendered as its builtin
  instead - `= "42"` where the Kotlin type is the class named by `type_name` - which does not compile. It now renders
  as `= MyCustomType("42".toLong())` for a `[custom_types.MyType]` config with `lift = "MyCustomType({}.toLong())"`,
  including when the custom type sits inside an `Option<T>`. Custom types without such a config are typealiases to
  their builtin and were already correct.
- A crate that uses types from another uniffi crate now initialises that crate too, so its callback interface vtables
  are registered before Rust can reach one ([uniffi #2343](https://github.com/mozilla/uniffi-rs/issues/2343)). Every
  namespace registers its vtables from its own lazily loaded `UniffiLib`, so passing a Kotlin implementation of a trait
  that belongs to a different crate used to reach Rust with that crate's vtable still unset - unless something had
  happened to call into that namespace first. Rust then called through a null vtable, which aborts the process rather
  than throwing, because the failing handle panics a second time while unwinding. The generated bindings now chain into
  `{package}.uniffiEnsureInitialized()` for each crate they use, and each binding exposes that function for its own
  namespace. This only covers crates that end up in the same shared library; see the known limitation below.

- Kotlin/Native links no longer fail with `duplicate symbol` when two uniffi modules share a Rust
  dependency. Each module's cinterop archive carries that dependency's object code, so the final link sees the same
  `#[no_mangle]` scaffolding symbols twice. Apple's linker takes the first definition and moves on; `lld` and the mingw
  driver rejected the link. The generated def files now pass `--allow-multiple-definition` on linux and mingw so every
  target behaves the way the Apple ones already did. Whether this trips at all depends on how rustc splits a crate into
  codegen units, so a project could link before this release and stop linking after an unrelated change.

### Removed

- Workaround for `spmForKmp` as it is no longer needed since version 1.9.5.
- `UniffiForeignFutureResultPointer` and `UniffiForeignFutureCompletePointer` from the runtime. Objects returned from
  an async function travel as a handle now, so uniffi no longer emits the pointer-shaped foreign-future result.

### Known limitations

- A trait declared `#[uniffi::export(with_foreign)]` (or `[Trait, WithForeign]`) cannot be implemented in Kotlin and
  passed to a function of a _different_ Gradle module. Each module builds its own shared library and links its Rust
  dependencies into it statically, so the shared crate's vtable slot exists once per library. The Kotlin package for
  that crate is generated once and registers the vtable with one library only, leaving the other library's slot unset -
  and a Rust-side call through it aborts the process. Implementing the trait and using it within the module that
  declares it works, as does passing records, enums and objects of a shared crate between modules. Generating the
  shared crate's bindings into the consuming module (`uniffi { generateBindingsForExternalCrates = true }`, without
  also depending on the module that declares it) keeps everything in one library and avoids this.

- Borrowed byte buffers (`&[u8]` in Rust, `[ByRef] bytes` in UDL, new in uniffi `0.32`) are not supported yet. They
  travel as a `ForeignBytes` that borrows the caller's buffer for the duration of the call, which needs the buffer
  pinned (Kotlin/Native) or copied into native memory (JNA) across the call - the bindgen rejects such an argument with
  a clear message instead. Take `Vec<u8>` / `bytes` instead.

## [1.1.1](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.1.1) - 2026-08-14

### Added

- A `swift-interop` example combining this plugin with `spmForKmp`

### Fixed

- Throwing methods of an object are annotated with `@Throws` on the JVM and Android actuals again. Only the generated
  interface carried the annotation, so the `throws` clause was missing from the class file and Java callers holding a
  reference of the object type could not catch the exception. The Kotlin/Native actuals stay unannotated, they inherit
  the filter from the interface and repeating it trips [KT-88548](https://youtrack.jetbrains.com/issue/KT-88548).

## [1.1.0](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.1.0) - 2026-08-07

### Added

- `cargo { ndkVersion = "..." }` to pin the NDK the android targets are built with. When unset the plugin picks the
  newest NDK under `$ANDROID_HOME/ndk`, then falls back to `$ANDROID_NDK_ROOT`.

### Changed

- Android support now requires the Android Kotlin Multiplatform library plugin (`com.android.kotlin.multiplatform.library`)
  and AGP `9`, replacing `com.android.library` together with `androidTarget { }`. The android configuration moves into
  the `kotlin { android { } }` block, see the [Android section](README.md#android) of the README.
- Update to Gradle `9.6.1`.
- The native libraries are handed to AGP through the variant API instead of by hooking into its task names. The
  per library `copyNativeLibs*` tasks are replaced by `mergeUniffiJvmResources`, `mergeUniffiAndroidJniLibs` and
  `mergeUniffiAndroidHostTestResources`.
- Update `jna` to `5.19.1`, `okio` to `3.18.1`, `kotlinx-coroutines` to `1.11.0` and `atomicfu` to `0.33.0`.
- The runtime is compiled against `compileSdk` `37` and no longer applies the `atomicfu` JVM bytecode transformation.

### Fixed

- `uniffi { formatCode = true }` had no effect - `ktlint` is now actually run over the generated bindings.

### Removed

- The `macosX64` Kotlin/Native target, which Kotlin deprecated ([native target tiers](https://kotl.in/native-targets-tiers)).
  The `x86_64-apple-darwin` rust target is still built for the jvm artifact, so intel macs keep working there.
- `kotlinx-datetime` from the dependencies added to `commonMain`. The generated bindings use `kotlin.time.Instant`
  and `kotlin.time.Duration` from the standard library.

## [1.0.15](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.0.15) - 2026-07-27

### Added

- Consumer ProGuard rules for the runtime.

### Changed

- Update to Kotlin `2.4.0`.
- Runtime and generated projects now share the same bindgen crate (`bindgen-bootstrap` was merged into `bindgen`).

## [1.0.14](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.0.14) - 2026-06-16

### Changed

- Don't bundle AGP with the plugin.
- Pin `smawk` version.

## [1.0.13](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.0.13) - 2026-06-16

### Fixed

- Omit the `Serializable` attribute if `generate_serializable_records` is disabled for all types.

## [1.0.12](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.0.12) - 2026-06-10

### Fixed

- Stream cargo output while building. This prevents occasional deadlogs around the output buffer.

## [1.0.11](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.0.11) - 2026-06-05

### Changed

- Skip serializer generation when disabled in the config.

## [1.0.10](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.0.10) - 2026-05-08

### Changed

- Use the UDL file for FFI generation as well.
- Skip building the library when using UDL.

## [1.0.8](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.0.8) - 2026-05-05

### Changed

- Set an explicit `minSdk` for the runtime.

## [1.0.7](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.0.7) - 2026-04-23

### Fixed

- Fall back to `~/.cargo/bin/` in the plugin and bindgen when cargo/rustup are not on the `PATH`.

## [1.0.0](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.0.0) - 2026-04-03

### Added

- Added support for External Types

### Fixed

- Updated the documentation to reflect the latest changes

## [0.4.3](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v0.4.3) - 2025-10-20

### Fixed

- Fix deserialization bug related to the one in `0.4.1`.

## [0.4.2](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v0.4.2) - 2025-10-13

### Fixed

- Fix `EnumTemplate` binding generation.

## [0.4.1](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v0.4.1) - 2025-10-10

### Fixed

- Fix struct deserialization bug

## [0.4.0](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v0.4.0) - 2025-09-29

### Changed

- Update to Kotlin `2.2.10`.
- Use `kotlin.time` instead of `kotlinx.datetime`.

## [0.3.5](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v0.3.5) - 2025-09-29

### Changed

- Allow disabling automatic dependency insertion.

## [0.3.0](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v0.3.0) - 2025-07-11

### Changed

The whole gradle plugin was rewritten from scratch:

- The `ch.ubique.uniffi.plugin` replaces the Cargo plugin (`io.gitlab.trixnity.cargo.kotlin.multiplatform`), UniFFI plugin (`io.gitlab.trixnity.uniffi.kotlin.multiplatform`), and Rust plugin (`io.gitlab.trixnity.rust.kotlin.multiplatform`).
- The new plugin provides a unified DSL for all configuration much faster configuration and build logic execution.

### Release Order Note

After `v0.7.0` was tagged, versioning was accidentally reset and subsequent releases were published as `v0.3.x` and then `v0.4.x`.

This file follows actual git tag chronology.

## [0.7.0](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v0.7.0) - 2025-04-04

### Added

- Add `useCross` option to use `cargo cross` instead of `cargo` for cross-compilation.

## [0.6.14](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v0.6.14) - 2025-02-20

### Fixed

- Minor fixes to generated code.

## [0.6.2](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v0.6.2) - 2025-02-17

### Changed

- `import_pointer_from` is now an array

## [0.6.1](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v0.6.1) - 2025-02-14

### Fixed

- Allow implicit `null`s in optional parameters.

## [0.6.0](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v0.6.0) - 2025-02-14

### Added

- Support for async traits (see [futures test](tests/uniffi/futures/))
- Support for sharing rust code & bindings between modules (see [multi-module test](tests/uniffi/multi-module/))

### Changed

- Renamed `CargoHost.current.hostTarget` to `RustHost.current.rustTarget`
- Renamed `io.gitlab.trixnity.gradle.rustlink.useRustUpLinker` to `io.gitlab.trixnity.gradle.rust.dsl.useRustUpLinker`
- `Uniffi Runtime` is now required as a depencecy (needs to be added manually as of right now)

## 2024-12-20

### Changed

- Update uniffi-rs to `v0.28.3`
- Added `Uniffi` prefix to callback factories

## 2024-12-20

### Changed

- Downgrade to JDK17

## 2024-12-20

### Changed

- Update bindings to support kotlin 2.1.0
- Update to JDK21
- JVM target jar now contains all shared libraries again. Use this to only build for your current platform:
  ```kotlin
  cargo {
      builds.jvm {
          jvm = (rustTarget == CargoHost.current.hostTarget)
      }
  }
  ```

## 2024-07-10

### Changed

- SourceSet naming: `nativeMain` -> `iosMain`

### Removed

- Support for building cross platform jars on MacOS
  - The Jar doesn't include Windows DLLs and Linux SOs anymore

## [0.1.0](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings/-/tags/v0.1.0) - 2023-11-25

### Added

- Support for `uniffi-rs@0.25.2`
