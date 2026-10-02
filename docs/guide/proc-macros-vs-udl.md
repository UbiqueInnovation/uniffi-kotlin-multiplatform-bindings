# Proc-macros and UDL

UniFFI gives you two ways to describe the interface you export:

- **Proc-macros**: annotate Rust items with `#[uniffi::export]`, `#[derive(uniffi::Record)]` and
  similar. This is the recommended way.
- **UDL**: describe the interface in a `.udl` file and generate the scaffolding from it in
  `build.rs`.

Both can be mixed in one crate. See UniFFI's own docs for the details of either:
[proc-macros](https://mozilla.github.io/uniffi-rs/0.32/proc_macro/index.html) and
[UDL](https://mozilla.github.io/uniffi-rs/0.32/udl/index.html).

On the Gradle side there are two matching modes. Call exactly one of them, otherwise configuration
fails with `Please call either 'generateFromLibrary' or 'generateFromUdl'.`

## `generateFromLibrary()` (recommended)

```kotlin
uniffi {
    generateFromLibrary()
}
```

The plugin builds the crate as a dynamic library for your machine and runs the generator in
UniFFI's *library mode*. UniFFI embeds the interface metadata in the compiled library, and this
includes the parts that come from a UDL file. So this mode works for proc-macro crates, UDL crates,
and crates that mix the two. Most fixtures in this repository are UDL crates generated this way.

Library mode also sees the crate's UniFFI dependencies, which is what makes
[multi-module projects](features/multi-module.md) and [external types](features/external-types.md)
work.

## `generateFromUdl { }`

```kotlin
uniffi {
    generateFromUdl {
        udlFile = layout.projectDirectory.file("src/my_crate.udl")
    }
}
```

The generator reads the UDL file directly, and the host library is not built just to generate
bindings. This is slightly faster, but proc-macro items in the crate are invisible to the
generator. Use it only for crates that are described entirely in UDL.

`udlFile` must be set. There is no default.

## A UDL crate

For completeness, this is what the Rust side of a UDL crate looks like:

```toml
# Cargo.toml
[dependencies]
uniffi = "0.32.0"

[build-dependencies]
uniffi = { version = "0.32.0", features = ["build"] }
```

```rust
// build.rs
fn main() {
    uniffi::generate_scaffolding("src/my_crate.udl").unwrap();
}
```

```rust
// src/commonMain/rust/lib.rs
pub fn add(a: i32, b: i32) -> i32 {
    a + b
}

uniffi::include_scaffolding!("my_crate");
```

```webidl
// src/my_crate.udl
namespace my_crate {
    i32 add(i32 a, i32 b);
};
```

Since `1.3.0`, both blocks accept `packageName`, which sets the Kotlin package of the bindings and takes
precedence over `package_name` in `uniffi.toml`:

```kotlin
uniffi {
    generateFromLibrary {
        packageName = "com.example.mylib"
    }
}
```

!!! note "Migrating"
    This property used to be called `namespace` and had no effect. Replace `namespace = ...` with
    `packageName = ...`. The UniFFI namespace itself always comes from the Rust side: the crate's
    library name, the argument to `setup_scaffolding!("...")`, or the UDL `namespace`.
