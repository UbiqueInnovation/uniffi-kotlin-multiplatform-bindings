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

## IDE sync

During an IntelliJ / Android Studio sync, the plugin generates the bindings so the IDE can resolve
them, but skips the Kotlin/Native Rust builds. The first real build after a sync compiles them.
