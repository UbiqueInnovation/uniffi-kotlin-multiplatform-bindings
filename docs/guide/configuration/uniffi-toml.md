# uniffi.toml

Settings that change the generated Kotlin go into a `uniffi.toml` next to the crate's
`Cargo.toml`. Every crate has its own file, so in a [multi-module project](../features/multi-module.md)
each module is configured separately.

```toml
package_name = "com.example.mylib"
generate_immutable_records = true
omit_checksums = true

[custom_types.Url]
type_name = "io.ktor.http.Url"
imports = ["io.ktor.http.Url"]
lift = "Url({})"
lower = "{}.toString()"
```

Unlike other UniFFI generators, the settings are at the top level of the file, not under
`[bindings.kotlin]`.

## Reference

| Key | Default | Description |
| --- | --- | --- |
| `package_name` | `uniffi.<namespace>` | Kotlin package of the generated code. `packageName` in the [Gradle DSL](gradle-dsl.md) takes precedence. |
| `cdylib_name` | the crate's library name | Name of the dynamic library JNA loads on JVM and Android. You only need it if you rename the library. |
| `generate_immutable_records` | `false` | Generate record fields as `val` instead of `var`. See [Records](../features/records-and-enums.md#immutable-records). |
| `mutable_records` | `[]` | Records that keep `var` fields when `generate_immutable_records` is on. |
| `generate_serializable_records` | `false` | Annotate records and enums with `@Serializable`. See [Serialization](../features/serialization.md). |
| `skip_serializer_for` | `[]` | Records and enums that are not annotated. |
| `custom_types.<Name>` | — | Kotlin type for a custom type. See [Custom types](../features/custom-types.md). |
| `external_packages` | inferred | Crate name → Kotlin package for types from other crates. See [External types](../features/external-types.md#package-names-of-external-crates). |
| `omit_checksums` | `false` | Skip the API checksum check at load time. See below. |
| `kotlin_target_version` | — | If `1.9.0` or newer, enums use `entries` instead of `values()`. |

## API checksums

On JVM and Android, the Rust library is loaded at runtime, so the bindings can be paired with the
wrong version of it. To catch that, every exported function has a checksum computed from its
signature. The bindings contain the checksums they were generated from, and compare them with the
ones the loaded library reports when it is first used. On a mismatch they throw
`UniFFI API checksum mismatch` instead of calling a function with the wrong arguments.

The check costs one FFI call per exported function at startup. The Gradle plugin always builds the
library and the bindings from the same sources, so you can skip it:

```toml
omit_checksums = true
```

The separate check of the UniFFI *contract version*, which catches a library built with a
different UniFFI version, always runs. Kotlin/Native links the library statically at build time,
so neither check exists there.
