# Changelog

## [Unreleased](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/compare/v1.0.0...HEAD)

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
