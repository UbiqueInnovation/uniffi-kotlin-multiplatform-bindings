# Internals

This part is for people working on this repository. It explains how a Rust crate becomes a Kotlin
Multiplatform library, and why the code is shaped the way it is. It assumes you have read the
[user guide](../guide/getting-started.md) and know what the plugin does from the outside.

## The short version

A Gradle plugin (`build-logic/gradle-plugin`) drives Cargo and a custom UniFFI binding generator
(`bindgen/`). The generator reads UniFFI's description of a crate and renders, from askama
templates, Kotlin for four source sets (`commonMain`, `jvmMain`, `androidMain`, `nativeMain`) plus
C headers for Kotlin/Native's cinterop. `commonMain` declares the public API, mostly as `expect`
declarations. Each platform source set provides the `actual` implementations and the FFI plumbing:
JNA on JVM and Android, cinterop on Native. Code that doesn't depend on the crate lives in a
separately published runtime library (`runtime/`, `ch.ubique.uniffi:runtime`).

## Repository layout

| Path | What |
| --- | --- |
| `build-logic/gradle-plugin/` | The Gradle plugin `ch.ubique.uniffi.plugin`. |
| `build-logic/conventions/` | Convention plugins used by the tests in this repository. |
| `bindgen/` | The binding generator `uniffi-bindgen-kotlin-multiplatform`, a Rust crate. |
| `runtime/` | The runtime library: hand-written Kotlin plus a tiny Rust crate. |
| `tests/uniffi/` | One Gradle module per test fixture. Most are ports of UniFFI's own fixtures. |
| `tests/runtime/` | Tests of the runtime library. |
| `examples/` | Small example projects, also built in CI. |

## Pages

| Page | Covers |
| --- | --- |
| [Architecture](architecture.md) | The three components and the build pipeline end to end. |
| [UniFFI primer](uniffi-primer.md) | The UniFFI concepts the rest of the code is built on. |
| [Gradle plugin](gradle-plugin.md) | Tasks, targets, how outputs reach the Kotlin source sets. |
| [Bindgen](bindgen.md) | The generator: config, `CodeType`, templates, headers. |
| [Runtime](runtime.md) | What lives in the runtime and why. |
| [Objects and handles](objects-and-handles.md) | How objects cross the FFI and who frees what. |
| [Callbacks](callbacks.md) | Kotlin implementations called from Rust: vtables and handle maps. |
| [Async](async.md) | Rust futures as `suspend` functions, and the reverse direction. |
| [External and remote types](external-and-remote-types.md) | Types from other crates, and multi-module builds. |
| [Testing](testing.md) | Fixtures, conventions, CI. |
| [Upgrading UniFFI](upgrading-uniffi.md) | What to check when moving to a new UniFFI version. |
