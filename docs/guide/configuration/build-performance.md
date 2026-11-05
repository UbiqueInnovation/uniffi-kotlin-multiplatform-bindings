# Build performance

Rust builds are usually the slowest part of a build with this plugin. These options help.

!!! note
`targetDirectory`, `rustcWrapper`, `rustcWorkspaceWrapper` and `androidDebugAbis` are
available since `1.2.2`.

## Build only what you need

- Debug builds only compile the host platform for JVM, and the host's ABI(s) for Android. Use
  release builds (`-PreleaseBuild=true`) only for publishing.
- Restrict the Android debug ABIs to your device with `-PandroidAbis=arm64-v8a` or
  `cargo { androidDebugAbis.add("arm64-v8a") }`. See [Targets](../targets.md#debug-abis).
- Only declare the Kotlin targets you need. Every Kotlin/Native target is a separate Rust build.

## Shared Cargo target directory

By default the plugin uses Cargo's normal target directory, as reported by `cargo metadata`.
Inside one Gradle build, all modules of a Cargo workspace already share it.

If the same Rust sources are built from **more than one Gradle build**, for example an SDK checkout
and an app that includes it through dependency substitution, point both builds at the same
directory:

```kotlin
cargo {
    targetDirectory = rootProject.layout.projectDirectory.dir("cargo-build")
}
```

Setting the `CARGO_TARGET_DIR` environment variable has the same effect, without changing build
scripts.

- The directory survives Gradle's `clean`. Delete it yourself when you need a fresh Rust build.
- Don't point it at a Gradle `build` directory. Kotlin and Android outputs stay project-local.
- In CI, persist it with the CI cache if separate jobs or runs should reuse it.

## Workspaces with dependent libraries

If the crates of several modules are in one Cargo workspace and depend on each other, as in a
[multi-module project](../features/multi-module.md), Cargo can end up recompiling them on every
build. Two things prevent that.

First, declare the crate types the plugin builds in each crate's `Cargo.toml`: `cdylib` for JVM and
Android, `staticlib` for Kotlin/Native, and `lib` if other crates depend on it:

```toml
[lib]
crate-type = ["lib", "cdylib", "staticlib"]
```

If one is missing, the plugin passes `--crate-type` to Cargo. A crate that declares a `cdylib` is
then built differently as the root of a build than as a dependency of another module's crate, and
is rebuilt with its dependents every time. The plugin warns about this.

Second, if Cargo still recompiles the shared crates whenever another module was built in between,
enable `buildWorkspaceLibraries` in every module of the workspace, for example from
the root project:

```kotlin
// build.gradle.kts of the root project
plugins {
    id("ch.ubique.uniffi.plugin") version "1.3.3" apply false
}

subprojects {
    plugins.withId("ch.ubique.uniffi.plugin") {
        extensions.configure<ch.ubique.uniffi.plugin.dsl.CargoExtension> {
            buildWorkspaceLibraries = true
        }
    }
}
```

Cargo resolves features per invocation, so the shared dependencies of `mod-a` and `mod-b` may be
compiled with different features depending on which one is built. With this option, every Cargo
call builds all libraries of the workspace that declare a `cdylib` or `staticlib`, so the features
are always the same. The first call builds them all, the following ones find them up to date.

## sccache

[sccache](https://github.com/mozilla/sccache) caches Rust compilation across target directories and
machines:

```kotlin
cargo {
    rustcWrapper = "sccache"
}
```

or set `RUSTC_WRAPPER=sccache` (or `RUSTC_WORKSPACE_WRAPPER=sccache`) before running Gradle. The
wrapper is passed to every Cargo invocation and is part of the task inputs.

## Prebuilt libraries

A release build compiles every Rust target one after another, in a single Gradle build. To build
them elsewhere instead, for example in parallel CI jobs, point the plugin at the finished libraries:

```kotlin
cargo {
    prebuiltLibraries = rootProject.layout.projectDirectory.dir("prebuilt")
}
```

The plugin then packages and links those, and generates the bindings from the host library, without
running Cargo. The directory is laid out like Cargo's target directory:

```
prebuilt/
  aarch64-apple-darwin/release/libfoo.dylib
  aarch64-linux-android/release/libfoo.so
  aarch64-apple-ios/release/libfoo.a
  aarch64-apple-ios/release/foo.native-static-libs
```

- Use the profile of the build: `debug`, or `release` with `-PreleaseBuild=true`.
- Kotlin/Native targets also need the system libraries the static library links against, in
  `<library name>.native-static-libs` next to it. rustc writes them when building the library with
  `cargo rustc ... -- --print native-static-libs=<file>`.
- Build the libraries with the same features as the plugin would. The plugin can't check them.
- A missing library fails the build.

## IDE sync

During an IntelliJ / Android Studio sync, the plugin generates the bindings so the IDE can resolve
them, but skips the Kotlin/Native Rust builds. The first real build after a sync compiles them.
