# Migrating

The full list of changes is in the
[changelog](https://github.com/UbiqueInnovation/oxikt/blob/main/CHANGELOG.md).

## From 1.3.x: OxiKT

Up to version 1.3.5, OxiKT was called UniFFI Kotlin Multiplatform Bindings and published under
`ch.ubique.uniffi`. Version 1.4.0 moves everything to `dev.oxikt`:

|                                  | Up to 1.3.5                | From 1.4.0                |
| -------------------------------- | -------------------------- | ------------------------- |
| Plugin ID                        | `ch.ubique.uniffi.plugin`  | `dev.oxikt.gradle.plugin` |
| Plugin classes, e.g. `BuildMode` | `ch.ubique.uniffi.plugin`  | `dev.oxikt.gradle.plugin` |
| Runtime                          | `ch.ubique.uniffi:runtime` | `dev.oxikt:runtime`       |
| Runtime classes                  | `uniffi.runtime`           | `dev.oxikt.runtime`       |
| Default package of the bindings  | `uniffi.<namespace>`       | `dev.oxikt.<namespace>`   |

### Plugin and runtime

Replace the plugin ID, and the imports of the plugin's classes in your build scripts:

```kotlin
import dev.oxikt.gradle.plugin.dsl.BuildMode // was ch.ubique.uniffi.plugin.dsl.BuildMode

plugins {
    id("dev.oxikt.gradle.plugin") version "1.4.0" // was ch.ubique.uniffi.plugin
}
```

The plugin adds the new runtime by itself. If you add it yourself (`addRuntime = false`), or have
it in a version catalog, use `dev.oxikt:runtime:1.4.0`.

If a repository has a content filter for `ch.ubique.uniffi`, filter for `dev.oxikt` instead. It
covers the runtime, the plugin and the plugin marker:

```kotlin
mavenContent { includeGroupAndSubgroups("dev.oxikt") }
```

The runtime's native library is now called `oxikt_runtime` instead of `uniffi_runtime`, for
example `liboxikt_runtime.so`. Update packaging rules and library overrides that name it, such as
`-Duniffi.component.oxikt_runtime.libraryOverride`.

### Kotlin packages

Crates without a package name in `uniffi.toml` or the [Gradle DSL](configuration/gradle-dsl.md)
now get the package `dev.oxikt.<namespace>` instead of `uniffi.<namespace>`. Either update your
imports, or keep the old package in the crate's `uniffi.toml`:

```toml
# for a crate with the namespace mylib
package_name = "uniffi.mylib"
```

This also applies to the other crates whose bindings are generated in the same run, such as
[external crates](features/external-types.md), so give each of them a `uniffi.toml` if you keep
the old packages.

Code that uses the runtime's classes directly imports them from `dev.oxikt.runtime` now. Update
ProGuard rules and other references to the `uniffi` packages too.

### Build modes

Version 1.4.0 also changes how Cargo is run. `cargo { buildMode }` replaces
`buildWorkspaceLibraries`, and defaults to workspace mode:

- Remove `buildWorkspaceLibraries = true`.
- Replace `buildWorkspaceLibraries = false` with `buildMode = BuildMode.Individual`.
- In workspace mode, each crate must declare the crate types its targets need in
  `[lib] crate-type`, and the plugin should be declared in the root project with `apply false`.

See [Build modes](configuration/build-performance.md#build-modes) for how the two modes differ.

## From 1.1.x (UniFFI 0.28)

Version 1.2.0 moved to UniFFI 0.32. Update your crate's `uniffi` dependency to `0.32.0`. The
marker for fake objects is now `NoHandle` (was `NoPointer`). See UniFFI's own changelog for the
Rust-side changes, for example `use_remote_type!` replacing `use_udl_record!` and friends.

## From 1.0.x

Android moved to the Android Kotlin Multiplatform library plugin and AGP 9, see
[Targets](targets.md#android). The `macosX64` Kotlin/Native target was removed. `kotlinx-datetime`
is no longer added, and the bindings use `kotlin.time.Instant` and `kotlin.time.Duration`.
