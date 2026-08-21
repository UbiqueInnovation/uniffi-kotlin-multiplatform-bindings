# Changelog

## [Unreleased](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/compare/v1.2.0...HEAD)

## [1.2.0](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/releases/tag/v1.2.0) - 2026-08-21

### Added

- `mutable_records` in `uniffi.toml`
- `omit_checksums` in `uniffi.toml`
- Borrowed bytes support
- `HashSet` support
- Methods on records and enums
- uniffi trait exports on records and enums: `Display`/`Debug`, `Eq`, `Hash`
- `Ord` on objects implements `Comparable<T>` and gets a `compareTo` backed by the Rust `Ord` impl

### Changed

- Update uniffi-rs to `v0.32.0` from `v0.28.3`
- The marker object for constructing an interface fake is called `NoHandle`, matching the upstream rename from `NoPointer`.
- The callback interface vtable gained a `uniffi_clone` entry and `uniffi_free` moved from last to first.
- The Rust toolchain moved to `1.97.1`.

### Fixed

- `#[uniffi(default = ...)]` on a field of an enum variant is honoured.
- A default value on a custom type is converted with that type's `lift` expression.
- A crate that uses types from another uniffi crate now initialises that crate too, so its callback interface vtables are registered before Rust can reach one ([uniffi #2343](https://github.com/mozilla/uniffi-rs/issues/2343)).
- Kotlin/Native links no longer fail with `duplicate symbol` when two uniffi modules share a Rust dependency.

### Removed

- Workaround for `spmForKmp` as it is no longer needed since version 1.9.5.

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
