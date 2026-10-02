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
build outputs, or a generator that doesn't match the runtime. Run a clean build. If you changed
the [generator source](configuration/gradle-dsl.md#where-the-generator-comes-from), make sure it
matches your plugin version.

**`IllegalStateException: ... object has already been destroyed`**: a method was called after
`destroy()`, `close()`, or after the end of a `use { }` block.

**`InternalException` with a Rust panic message**: the Rust code panicked. See
[Errors](features/errors.md#panics-and-unexpected-errors).

**`UnsatisfiedLinkError` / `Unable to load library`** on JVM: the library for the current platform
is not on the classpath. Debug JVM builds only include the host platform. Build with
`-PreleaseBuild=true` to include all of them.

**Crashes or memory corruption when passing strings, records or lists to Rust**: check whether
one of your crates, or a dependency, sets a `#[global_allocator]`. That isn't supported, see
[Requirements](requirements.md#dont-use-a-custom-global-allocator).

**The process aborts (exit code 134) when Rust calls a Kotlin callback**: most likely a Kotlin
implementation of a trait from another module's crate was passed across modules. See
[Multi-module projects](features/multi-module.md#limitations) and [#35](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/issues/35).

## Swift interop with spmForKmp

On Apple targets this plugin works together with
[spmForKmp](https://github.com/frankois944/spm4Kmp), which lets Kotlin call Swift code through
cinterop. Use spmForKmp `1.9.5` or newer. Older versions configured their cinterop in a way that
conflicted with this plugin's cinterop.

```kotlin
plugins {
    kotlin("multiplatform")
    id("io.github.frankois944.spmForKmp") version "1.9.5"
    id("ch.ubique.uniffi.plugin")
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.swiftPackageConfig(cinteropName = "swiftGreeter") {
            minIos = "16.4"
        }
    }
}
```

If you're stuck on spmForKmp older than `1.9.5`, add this workaround to the build script
([spm4Kmp#326](https://github.com/frankois944/spm4Kmp/issues/326)). It points this plugin's
cinterop back at its own def file after spmForKmp has reconfigured it:

```kotlin
import ch.ubique.uniffi.plugin.tasks.GenerateDefFileTask
import ch.ubique.uniffi.plugin.tasks.GenerateDummyDefFileTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

afterEvaluate {
    kotlin.targets.withType<KotlinNativeTarget>().configureEach {
        val defFile = uniffiDefFileFor(targetName)

        compilations.getByName("main").cinterops.named("uniffi-cinterop") {
            definitionFile.set(defFile)

            tasks.named(interopProcessingTaskName) {
                dependsOn(defFile)
            }
        }
    }
}

/**
 * The def file the plugin feeds to its cinterop for [targetName]. During an IDE sync the plugin
 * registers a single GenerateDummyDefFileTask shared by every target, otherwise one
 * GenerateDefFileTask per target.
 */
fun uniffiDefFileFor(targetName: String): Provider<RegularFile> {
    // idea.sync.active is set by IntelliJ-based IDEs during a sync
    val isSync = providers.systemProperty("idea.sync.active").map(String::toBoolean).getOrElse(false)

    return if (isSync) {
        tasks.named<GenerateDummyDefFileTask>("generateDummyDefFile")
            .flatMap { it.outputFile }
    } else {
        tasks.named<GenerateDefFileTask>(
            "generateDefFileFor${targetName.replaceFirstChar(Char::uppercaseChar)}"
        ).flatMap { it.outputFile }
    }
}
```

See [`examples/swift-interop`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/examples/swift-interop)
for a module that calls both Rust and Swift.

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
