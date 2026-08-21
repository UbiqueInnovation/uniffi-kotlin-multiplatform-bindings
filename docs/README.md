# Internal development documentation

How this repository turns a Rust crate into Kotlin Multiplatform bindings, and how the
mechanisms that dominate the design actually work.

These documents describe the code as it is on `feature/uniffi-0.32.0-update` (uniffi 0.32.0).
They are written for someone changing the generator or the runtime — not for consumers, who
should read the top-level [`README.md`](../README.md) instead.

| Document | Covers |
| --- | --- |
| [Deep dive](deep-dive.md) | The runtime mechanisms in one place: objects and their methods, methods on records/enums, async in both directions, callback interfaces and trait interfaces, who frees what, and what a multi-module build actually requires — including why the Kotlin/Native vtable cell diverges. Start here if you want the whole picture. |
| [Architecture](architecture.md) | The full pipeline: Gradle plugin → cargo → bindgen → four Kotlin source sets + C headers → cinterop/JNA. Template layout, the `expect`/`actual` split, `FfiConverter`, the runtime module. |
| [Handles](handles.md) | How object references cross the FFI as opaque 64-bit handles, the clone/free protocol, the call counter, cleaners, bidirectional trait-interface handles and the low-bit tag. |
| [Async](async.md) | Rust futures → `suspend` functions, the poll loop, continuation handle map, cancellation, and the reverse direction (async callback interface methods → `ForeignFuture`). |
| [External and remote types](external-and-remote-types.md) | Multi-crate builds: how a type from another crate is resolved to a Kotlin package, the `RustBuffer{Name}` typealiases, remote types, custom types, and init chaining across crates. |

Also worth reading, and not duplicated here:

- [`.claude/uniffi-0.32-update.md`](../.claude/uniffi-0.32-update.md) — the 0.28.3 → 0.32.0 port
  log: what changed, what is deliberately divergent from upstream, what is missing, and the
  gotchas. Where these documents say "known gap", that file has the detail.
- [`CHANGELOG.md`](../CHANGELOG.md) — consumer-facing migrations.

## The one-paragraph version

A Gradle plugin (`build-logic/gradle-plugin`) drives cargo and a custom `uniffi-bindgen`
(`bindgen/`). The bindgen reads uniffi's `ComponentInterface` and renders **five** outputs per
crate from askama templates: `commonMain`, `jvmMain`, `androidMain` and `nativeMain` Kotlin, plus
C headers for Kotlin/Native's cinterop. Common declares the public API as `expect` declarations;
each platform source set supplies the `actual` implementation and the FFI plumbing — JNA on
JVM/Android, cinterop on Native. Shared helper code that does not depend on the crate lives in a
separately published `ch.ubique.uniffi:runtime` module (`runtime/`), whose own Kotlin is
hand-written and mirrors the `generic/ffi/` templates.
