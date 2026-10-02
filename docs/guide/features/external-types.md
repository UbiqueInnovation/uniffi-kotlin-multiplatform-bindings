# External types

An *external type* is a UniFFI type defined in another crate that your crate uses, for example a
record from a dependency used as a function argument. See UniFFI's
[documentation on external types](https://mozilla.github.io/uniffi-rs/0.32/types/remote_ext_types.html).

There are two ways to get Kotlin bindings for the other crate.

## Prefer a separate module

If you control the other crate, give it its own Gradle module and depend on that. This is the
[multi-module setup](multi-module.md), and it is the recommended approach: consumers can pick the
modules they need, and every type exists exactly once.

## Generate the bindings together

If the other crate is a third-party crate that uses UniFFI, and there is no Kotlin module for it,
let your module generate its bindings too:

```kotlin
uniffi {
    generateFromLibrary()
    generateBindingsForExternalCrates = true
}
```

The generator then writes Kotlin for every UniFFI crate linked into your library, each in its own
package. The other crate's package name comes from its own `uniffi.toml` if it has one, and is
`uniffi.<namespace>` otherwise.

!!! warning
    Only one module on a classpath may generate a given crate's bindings. If two modules both
    generate bindings for the same crate, the application gets duplicate classes.

## Package names of external crates

Within one generation run, the generator knows every crate's package and imports external types
from the right one. If a dependency's bindings were generated elsewhere with a non-default package
name that this run can't see, map it by crate name in `uniffi.toml`:

```toml
[external_packages]
other_crate = "com.example.other"
```

## Remote types

A *remote type* is a type from a crate that does not use UniFFI at all, like `url::Url`. Remote
types are handled entirely in Rust. You either describe the type as a
[custom type](custom-types.md) with `remote`, or, if another UniFFI crate already did that, reuse
its definition:

```rust
uniffi::use_remote_type!(other_uniffi_crate::Url);
```

In UDL, a type from a third-party crate that you describe yourself needs the `[Remote]` attribute.

## Fixtures

- [`tests/uniffi/ext-types`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/ext-types)
