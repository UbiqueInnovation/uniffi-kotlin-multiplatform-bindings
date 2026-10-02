# Multi-module projects

You can split your Rust code into several crates, each with its own Gradle module and its own
bindings, and use the types of one module in another. Objects and records pass between modules
directly, with no conversion or copying beyond the usual FFI transfer.

This is the main thing that sets this project apart from other UniFFI Kotlin generators. It lets
you publish a set of composable Kotlin libraries, and lets consumers pick only the ones they need.

The reference setup is
[`tests/uniffi/multi-module`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/multi-module):

```
rust-common   shared types: TestRecord, TestObject
├── mod-a     uses rust-common's types in its own API
├── mod-b     uses rust-common's types in its own API
└── combined  a plain Kotlin module (no Rust) that depends on all three, like an app would
```

## Setup

### The shared crate

An ordinary UniFFI crate. It needs the `lib` crate type so other crates can depend on it:

```toml
# rust-common/Cargo.toml
[lib]
name = "uniffi_kmm_fixture_multi_rust_common"
crate-type = ["lib", "cdylib", "staticlib"]
path = "src/commonMain/rust/lib.rs"
```

```rust
// rust-common/src/commonMain/rust/lib.rs
#[derive(uniffi::Record)]
pub struct TestRecord {
    pub int: i64,
    pub str: String,
    pub vec: Vec<i64>,
}

#[derive(uniffi::Object)]
pub struct TestObject {
    pub name: String,
}

uniffi::setup_scaffolding!("rust_common");
```

```toml
# rust-common/uniffi.toml
package_name = "rust_common"
```

### A dependent crate

Depend on the shared crate in Cargo, and use its types:

```toml
# mod-a/Cargo.toml
[dependencies]
uniffi = "0.32.0"
uniffi-kmm-fixture-multi-rust-common = { path = "../rust-common" }
```

```rust
// mod-a/src/commonMain/rust/lib.rs
use uniffi_kmm_fixture_multi_rust_common::{TestObject, TestRecord};

#[uniffi::export]
pub fn greet(obj: &TestObject) -> String {
    format!("Hello {}!", obj.get_name())
}

uniffi::setup_scaffolding!("module_a");
```

Then mirror the dependency in Gradle:

```kotlin
// mod-a/build.gradle.kts
uniffi {
    generateFromLibrary()
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":rust-common"))
    }
}
```

Use `api` rather than `implementation` when the shared types appear in your module's public API,
which they usually do.

Each module needs its own `package_name`.

### Using it

```kotlin
import module_a.greet
import module_b.createTestObject

val obj = createTestObject("John")   // created by module_b
greet(obj)                           // "Hello John!", handled by module_a
```

`module_a` doesn't generate a `TestObject` class of its own. It imports `rust_common.TestObject`,
so the same Kotlin type is used everywhere.

## How it works

Each module generates bindings **only for its own crate** and imports the Kotlin types of the
crates it depends on. This happens automatically, because `generateBindingsForExternalCrates` is
`false` by default. Don't turn it on in a multi-module project. Every module would then
generate its own copy of the shared types, and you would get duplicate classes.

On the native side, each module still builds its own library, and the shared crate's Rust code is
linked into each of them. That is not a problem for data. Records are serialised on every crossing.
Objects are plain pointers, and every library contains identical code for the shared crate, so it
doesn't matter which library's copy calls a method on an object or frees it.

For Kotlin/Native targets, all the static libraries end up in the same binary, so the shared
crate's symbols are present several times. The plugin passes `--allow-multiple-definition` to
the Linux and Windows linkers to allow this. Apple's linker accepts it by default.

## Limitations

- **Foreign trait implementations across modules don't work.** A Kotlin implementation of a
  `with_foreign` trait (or callback interface) declared in `rust-common` can be passed to
  functions of `rust-common` itself, but not to functions of `mod-a`. Each library holds its own
  copy of the trait's callback table, and only `rust-common`'s copy is initialised from Kotlin. The
  call aborts the process. Rust implementations of such traits are not affected. This will be fixed
  in future versions.
- **Build all modules against the same version of the shared crate.** Each module embeds its own
  compiled copy of `rust-common`. If `mod-a` and `mod-b` are built against different versions of
  it, they disagree about the layout of shared types, and nothing checks for that at runtime.

## External crates you don't control

If your crate depends on a third-party crate that also uses UniFFI, and that crate isn't available
as its own Kotlin module, see [External types](external-types.md).
