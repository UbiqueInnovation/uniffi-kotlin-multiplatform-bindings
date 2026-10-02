# Troubleshooting

## Configuration errors

**`Please set 'kotlin.mpp.enableCInteropCommonization=true' in gradle.properties`**:
you have a Kotlin/Native target. See [Getting started](getting-started.md#3-enable-cinterop-commonization).

**`Please call either 'generateFromLibrary' or 'generateFromUdl'.`**: the `uniffi { }` block
needs one of the two. See [Proc-macros and UDL](proc-macros-vs-udl.md).

**`Kotlin Multiplatform Plugin is required`**: apply `kotlin("multiplatform")` in the same
module.

**`Unhandled target: ...`**: the module declares a Kotlin target the plugin can't build Rust for.
See [Targets](targets.md) for the supported list.

**`Unsupported Android ABI(s): ...`**: `androidDebugAbis` or `-PandroidAbis` contains something
other than `arm64-v8a`, `armeabi-v7a` or `x86_64`.

## Build errors

**NDK not found**: install an NDK through the SDK manager, or set `ANDROID_NDK_ROOT`, or pin an
installed version with `cargo { ndkVersion = "..." }`. See [Targets](targets.md#ndk).

**Missing Rust target**: the plugin runs `rustup target add` automatically. If that fails, for
example because `rustup` is not available, install the target yourself.

**Kotlin/Native link errors in Rust code** (unknown relocations, unsupported object file
features): Kotlin/Native's bundled linker is older than your Rust toolchain. Use
[`useRustUpLinker()`](targets.md#linking-with-rusts-linker) for the affected compilation.

**Unresolved references to `cinterop.*` in the generated `nativeMain` code**: cinterop
commonization is off, or the IDE hasn't run a sync since it was turned on.

## Runtime errors

**`UniFFI contract version mismatch`** or **`UniFFI API checksum mismatch`** (JVM and Android):
the bindings were generated for a different library than the one that was loaded. Usually stale
build outputs, or a generator that doesn't match the runtime. Run a clean build. If you use the
default generator source, [pin it to your plugin version](configuration/gradle-dsl.md#where-the-generator-comes-from).

**`IllegalStateException: ... object has already been destroyed`**: a method was called after
`destroy()`, `close()`, or after the end of a `use { }` block.

**`InternalException` with a Rust panic message**: the Rust code panicked. See
[Errors](features/errors.md#panics-and-unexpected-errors).

**`UnsatisfiedLinkError` / `Unable to load library`** on JVM: the library for the current platform
is not on the classpath. Debug JVM builds only include the host platform. Build with
`-PreleaseBuild=true` to include all of them.

**The process aborts (exit code 134) when Rust calls a Kotlin callback**: most likely a Kotlin
implementation of a trait from another module's crate was passed across modules. See
[Multi-module projects](features/multi-module.md#limitations).

## Migrating

**From 1.0.x**: Android moved to the Android Kotlin Multiplatform library plugin and AGP 9, see
[Targets](targets.md#android). The `macosX64` Kotlin/Native target was removed. `kotlinx-datetime`
is no longer added, and the bindings use `kotlin.time.Instant` and `kotlin.time.Duration`.

**From 1.1.x (UniFFI 0.28)**: version 1.2.0 moved to UniFFI 0.32. Update your crate's `uniffi`
dependency to `0.32.0`. The marker for fake objects is now `NoHandle` (was `NoPointer`). See
UniFFI's own changelog for the Rust-side changes, for example `use_remote_type!` replacing
`use_udl_record!` and friends.

The full list of changes is in the
[changelog](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/blob/main/CHANGELOG.md).
