# Changelog

## [Unreleased](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings/compare/v0.1.0...HEAD)

## [0.1.0](https://gitlab.com/trixnity/uniffi-kotlin-multiplatform-bindings/-/tags/v0.1.0) - 2023-11-25

### Added

- Support for `uniffi-rs@0.25.2`

## 0.3.0 - 2024-07-10

### Changed

- SourceSet naming: `nativeMain` -> `iosMain`

### Removed

- Support for building cross platform jars on MacOS
    - The Jar doesn't include Windows DLLs nad Linux SOs anymore

