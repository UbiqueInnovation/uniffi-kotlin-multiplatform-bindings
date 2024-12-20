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
    - The Jar doesn't include Windows DLLs and Linux SOs anymore

## 0.4.0 - 2024-12-20

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

## 0.4.1 - 2024-12-20

### Changed

- Downgrade to JDK17

## 0.4.2 - 2024-12-20

### Changed

- Update uniffi-rs to `v0.28.3`
- Added `Uniffi` prefix to callback factories
