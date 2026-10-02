# Targets

The plugin looks at the Kotlin targets you declare in `kotlin { }` and builds the Rust crate for
each of them. You don't have to configure targets separately for the plugin.

| Kotlin target                 | Rust target(s)                                           | Library | Delivered as                 |
| ----------------------------- | -------------------------------------------------------- | ------- | ---------------------------- |
| `jvm()`                       | host (debug), all desktop targets (release)              | dynamic | JVM resources, loaded by JNA |
| `android { }`                 | see [Android](#android)                                  | dynamic | `jniLibs`, loaded by JNA     |
| `iosArm64()`                  | `aarch64-apple-ios`                                      | static  | cinterop                     |
| `iosSimulatorArm64()`         | `aarch64-apple-ios-sim`                                  | static  | cinterop                     |
| `iosX64()`                    | `x86_64-apple-ios`                                       | static  | cinterop                     |
| `macosArm64()`                | `aarch64-apple-darwin`                                   | static  | cinterop                     |
| `linuxX64()` / `linuxArm64()` | `x86_64-unknown-linux-gnu` / `aarch64-unknown-linux-gnu` | static  | cinterop                     |
| `mingwX64()`                  | `x86_64-pc-windows-gnu`                                  | static  | cinterop                     |

A Kotlin target that is not in this table fails the configuration with `Unhandled target`.

## Debug and release builds

Everything is built with Cargo's `dev` profile by default. To build with `--release`, pass the
Gradle property `releaseBuild`:

```bash
./gradlew publish -PreleaseBuild=true
```

The property also changes **which** Rust targets are built for JVM and Android:

|         | Debug                          | Release                                                      |
| ------- | ------------------------------ | ------------------------------------------------------------ |
| JVM     | the host only                  | `aarch64`/`x86_64` for macOS and Linux, `x86_64` for Windows |
| Android | the host architecture's ABI(s) | `arm64-v8a`, `armeabi-v7a`, `x86_64`                         |

So a debug JVM artifact only works on the machine that built it, and a release build needs
toolchains for every desktop platform.

## JVM

```kotlin
kotlin {
    jvm()
}
```

The dynamic libraries are copied into the `jvmMain` resources under JNA's platform prefix (for
example `darwin-aarch64/libfoo.dylib`), so JNA finds them on the classpath at runtime. To load a
different library file instead, set the system property
`uniffi.component.<namespace>.libraryOverride` to its name or path.

For a release build, Cargo needs a C toolchain and linker for each foreign platform. On macOS,
the CI uses [`messense/macos-cross-toolchains`](https://github.com/messense/homebrew-macos-cross-toolchains)
and `mingw-w64` and sets the usual Cargo variables, for example:

```bash
CC_x86_64_unknown_linux_gnu=x86_64-linux-gnu-gcc
AR_x86_64_unknown_linux_gnu=x86_64-linux-gnu-ar
CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER=x86_64-linux-gnu-gcc
```

See [`.github/workflows/publish.yml`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/blob/main/.github/workflows/publish.yml)
for the full set. Alternatively, a target can be built with
[`cross`](https://github.com/cross-rs/cross):

```kotlin
cargo {
    compilations.linuxX64 { useCross = true }
}
```

For a JVM-only project, see
[`examples/jvm-only`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/examples/jvm-only).

## Android

Android support is built on the Android Kotlin Multiplatform library plugin (AGP 9). Apply it next
to the Kotlin Multiplatform plugin and configure Android inside `kotlin { }`:

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library") version "9.3.1"
    id("ch.ubique.uniffi.plugin") version "1.2.1"
}

kotlin {
    android {
        namespace = "com.example.quickstart"
        compileSdk = 36
        minSdk = 21
    }
}
```

The libraries are handed to AGP as generated `jniLibs`. If you enable Android host tests with
`withHostTest { }`, the plugin also builds the crate for your machine and adds it to the host
test resources, so unit tests can call Rust without a device.

!!! note "Migrating from 1.0.x"
Older versions used `com.android.library` with `androidTarget { }` and a top-level
`android { }` block. Both are replaced by the setup above. `minSdk` and `compileSdk` are now set
directly in `kotlin { android { } }` instead of in a `defaultConfig { }` block.

### NDK

The Android targets are compiled with the NDK's clang. By default the plugin picks the newest NDK
in `$ANDROID_HOME/ndk`, and falls back to `$ANDROID_NDK_ROOT`. To pin a version:

```kotlin
cargo {
    ndkVersion = "28.1.13356709"
}
```

### Debug ABIs

In debug builds the plugin compiles for the ABI(s) matching your machine: `arm64-v8a` on ARM hosts,
and `x86_64` plus `arm64-v8a` on x86 hosts. To compile only what your device needs:

```bash
./gradlew :app:assembleDebug -PandroidAbis=arm64-v8a
```

or, permanently:

```kotlin
cargo {
    androidDebugAbis.add("arm64-v8a")
}
```

Both accept `arm64-v8a`, `armeabi-v7a` and `x86_64`. Release builds always include all three.

## Kotlin/Native

Each native target links a static library through cinterop. The plugin generates a `.def` file per
target that points at the library and the generated C headers, and adds a cinterop named
`uniffi-cinterop` to the target's `main` compilation.

Remember to set `kotlin.mpp.enableCInteropCommonization=true`, see
[Getting started](getting-started.md#3-enable-cinterop-commonization).

Apple targets can only be built on macOS. If the same build script also runs on Linux or Windows
CI, guard them:

```kotlin
import ch.ubique.uniffi.plugin.model.RustHost

kotlin {
    if (RustHost.Platform.MacOS.isCurrent) {
        iosArm64()
        iosSimulatorArm64()
        macosArm64()
    }
}
```

### Linking with Rust's linker

The linker bundled with Kotlin/Native is sometimes older than the LLVM your Rust toolchain uses,
and fails to link the Rust objects. `useRustUpLinker()` makes a compilation link with the `lld`
that ships with your active Rust toolchain instead:

```kotlin
import ch.ubique.uniffi.plugin.extensions.useRustUpLinker

kotlin {
    mingwX64 {
        compilations.getByName("test") {
            useRustUpLinker()
        }
    }
}
```

The examples and tests in this repository use it for `mingwX64` test binaries and for the Apple
targets.
