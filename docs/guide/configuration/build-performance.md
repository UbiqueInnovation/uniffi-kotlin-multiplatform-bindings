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

## Build modes

The plugin runs Cargo in one of two modes, set with `cargo { buildMode }`.

### Workspace mode

The default. The modules of a Cargo workspace share one `cargo build` per Rust target and profile.
It builds the crates of all modules that build that target, with the
[features](gradle-dsl.md#cargo) of all of them, and the other modules reuse its output. Cargo
resolves the features once for all crates, so every library is built with the same features, and
nothing is rebuilt because another module was built in between.

Each crate must declare the crate types its targets need, or the build fails: `cdylib` for JVM and
Android, `staticlib` for Kotlin/Native, and `lib` if other crates depend on it:

```toml
[lib]
crate-type = ["lib", "cdylib", "staticlib"]
```

What it costs:

- Cargo builds every crate type a crate declares, on every target. Android builds get a
  `staticlib` they don't need, for example.
- Building one module builds the crates of all modules for that target.
- Each library is built with the features that all modules need.
- The crates and features are collected while Gradle configures the modules, so all of them have
  to be configured. That is Gradle's default, but not the case with configure-on-demand.

On Kotlin/Native targets, each module also builds its crate on its own, to ask rustc which system
libraries to link. That build only has the features of the module's own crate. It can compile some
dependencies a second time, and in rare cases it misses a system library that only another
module's features bring in. Add such a library to the binary's `linkerOpts` yourself.

### Individual mode

Each module runs `cargo rustc` for its own crate, with exactly the crate types its targets need,
so the crate doesn't have to declare them. Building a module only builds what that module needs.

```kotlin
import ch.ubique.uniffi.plugin.dsl.BuildMode

cargo {
    buildMode = BuildMode.Individual
}
```

Keep in mind:

- A crate that other crates of the workspace depend on must not declare a `cdylib`. Cargo keeps a
  single build of it per target, which the module's own build and the builds of the crates
  depending on it would keep replacing. The build fails if a module's crate depends on one.
  Without a `[lib] crate-type`, a crate is a plain `lib`, which is fine.
- Cargo resolves features for each crate on its own. If the crates need different features of
  shared dependencies, Cargo compiles those dependencies once per set of features. Full builds get
  slower and the target directory bigger. [cargo-hakari](https://docs.rs/cargo-hakari) can unify the
  features across the workspace.
- Each library contains its own copy of the crates it depends on, and objects pass between modules
  as pointers. If a shared crate's types change their layout with a feature, build it with the same
  features for all modules. Otherwise a module reads another module's objects with the wrong
  layout. See [Multi-module projects](../features/multi-module.md#limitations).
- Plain `cargo build` only builds the crate types a crate declares. Tools outside Gradle that need a
  `cdylib` or `staticlib` have to ask for it with `cargo rustc --crate-type`.

Use the same mode in all modules of a workspace, for example from the root project:

```kotlin
// build.gradle.kts of the root project
subprojects {
    plugins.withId("ch.ubique.uniffi.plugin") {
        extensions.configure<ch.ubique.uniffi.plugin.dsl.CargoExtension> {
            buildMode = ch.ubique.uniffi.plugin.dsl.BuildMode.Individual
        }
    }
}
```

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
  `cargo rustc ... -- --print native-static-libs=<file>`. In individual mode, the plugin's own
  builds write this file too, so their Cargo target directory has the layout of a prebuilt one.
- Build the libraries with the same features as the plugin would. In workspace mode, that is one
  `cargo build` with the crates of all modules. The plugin can't check them.
- A missing library fails the build.

## IDE sync

During an IntelliJ / Android Studio sync, the plugin generates the bindings so the IDE can resolve
them, but skips the Kotlin/Native Rust builds. The first real build after a sync compiles them.
