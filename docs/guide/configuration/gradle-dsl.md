# Gradle DSL

The plugin adds two extensions: `uniffi { }` for binding generation, and `cargo { }` for the Rust
build.

## `uniffi { }`

```kotlin
uniffi {
    generateFromLibrary()                    // or generateFromUdl { udlFile = ... }

    formatCode = false
    addRuntime = true
    addDependencies = true
    generateBindingsForExternalCrates = false

    bindgenFromGitTag(
        "https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings.git",
        "v1.2.1",
    )
}
```

| Option                              | Default | Description                                                                                                                                         |
| ----------------------------------- | ------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| `generateFromLibrary()`             | —       | Generate bindings from the compiled library. One of the two `generateFrom*` calls is required. See [Proc-macros and UDL](../proc-macros-vs-udl.md). |
| `generateFromUdl { udlFile = ... }` | —       | Generate bindings from a UDL file. `udlFile` is required.                                                                                           |
| `formatCode`                        | `false` | Run `ktlint --format` over the generated bindings. `ktlint` must be on `PATH`. Problems it can't fix are reported as a warning.                     |
| `addRuntime`                        | `true`  | Add `ch.ubique.uniffi:runtime` to `commonMain`. See [Dependencies](dependencies.md).                                                                |
| `addDependencies`                   | `true`  | Add the libraries the generated code needs. See [Dependencies](dependencies.md).                                                                    |
| `generateBindingsForExternalCrates` | `false` | Also generate bindings for other UniFFI crates linked into the library. See [External types](../features/external-types.md).                        |

### Where the generator comes from

The plugin installs the binding generator (`uniffi-bindgen-kotlin-multiplatform`) with
`cargo install` the first time it is needed. You can choose the source:

| Function                                       | Installs from                                                |
| ---------------------------------------------- | ------------------------------------------------------------ |
| `bindgenFromGitTag(repository, tag)`           | a Git tag                                                    |
| `bindgenFromGitRevision(repository, revision)` | a Git commit                                                 |
| `bindgenFromGitBranch(repository, branch)`     | the head of a Git branch, checked for updates on every build |
| `bindgenFromGit(repository)`                   | the default branch, checked for updates on every build       |
| `bindgenFromRegistry(packageName, version)`    | the Cargo registry                                           |
| `bindgenFromPath(directory)`                   | a local checkout, for developing the generator               |

**By default the generator is installed from the default branch of this repository**, not from
the tag matching your plugin version. For reproducible builds, pin it to the tag of the plugin
version you use, as in the example above. The generator must match the runtime version, which is
always the plugin version.

The installed generator is shared by all modules of a build and kept in the root project's
`build/uniffi/bindgen/` directory.

## `cargo { }`

```kotlin
cargo {
    packageDirectory = layout.projectDirectory
    targetDirectory = rootProject.layout.projectDirectory.dir("cargo-build")
    rustcWrapper = "sccache"
    ndkVersion = "28.1.13356709"
    androidDebugAbis.add("arm64-v8a")

    compilations.linuxArm64 {
        useCross = true
    }
}
```

| Option                                      | Default                    | Description                                                                                            |
| ------------------------------------------- | -------------------------- | ------------------------------------------------------------------------------------------------------ |
| `packageDirectory`                          | the project directory      | The directory containing `Cargo.toml`.                                                                 |
| `targetDirectory`                           | from `cargo metadata`      | Cargo's target directory. See [Build performance](build-performance.md#shared-cargo-target-directory). |
| `rustcWrapper`                              | `$RUSTC_WRAPPER`           | Passed to Cargo as `RUSTC_WRAPPER`, for example `sccache`.                                             |
| `rustcWorkspaceWrapper`                     | `$RUSTC_WORKSPACE_WRAPPER` | Passed to Cargo as `RUSTC_WORKSPACE_WRAPPER`.                                                          |
| `ndkVersion`                                | newest installed           | The NDK used for Android targets. See [Targets](../targets.md#ndk).                                    |
| `androidDebugAbis`                          | host ABI(s)                | Android ABIs built in debug builds. See [Targets](../targets.md#debug-abis).                           |
| `compilations.<target> { useCross = true }` | `false`                    | Build that Rust target with [`cross`](https://github.com/cross-rs/cross) instead of `cargo`.           |

`compilations` has shortcuts for `iosArm64`, `iosX64`, `macosArm64`, `macosX64`, `linuxArm64`,
`linuxX64`, `windowsX64`, `androidArm64`, `androidArmV7` and `androidX64`.

## Gradle properties

| Property                         | Effect                                                                                                    |
| -------------------------------- | --------------------------------------------------------------------------------------------------------- |
| `-PreleaseBuild=true`            | Build Rust in release mode, and for all platforms. See [Targets](../targets.md#debug-and-release-builds). |
| `-PandroidAbis=arm64-v8a,x86_64` | Android ABIs for debug builds, if `androidDebugAbis` is not set.                                          |

## Tasks

| Task                                     | Does                                                                              |
| ---------------------------------------- | --------------------------------------------------------------------------------- |
| `buildBindings`                          | Generate the Kotlin bindings into `build/uniffi/bindings/`. Runs on IDE sync too. |
| `installBindgen`                         | Install the binding generator.                                                    |
| `cargoBuild<RustTarget><Debug\|Release>` | Build the crate for one Rust target, e.g. `cargoBuildAarch64AppleDarwinDebug`.    |
| `buildLibraryForBindings`                | Build the host library the bindings are generated from.                           |

You don't normally run these yourself. The Kotlin compile tasks depend on them.
