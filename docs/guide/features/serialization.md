# Serialization

The generator can annotate records and enums with `@kotlinx.serialization.Serializable`, so they
can be encoded with `kotlinx.serialization` directly:

```toml
# uniffi.toml
generate_serializable_records = true
```

The module also needs the serialization compiler plugin and a format:

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.4.0"
    id("ch.ubique.uniffi.plugin")
}

kotlin.sourceSets.commonMain.dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:<version>")
}
```

```rust
#[derive(uniffi::Record)]
pub struct Values {
    pub a: i64,
    pub b: i64,
}
```

```kotlin
Json.encodeToString(Values(4, 2))                   // {"a":4,"b":2}
Json.decodeFromString<Values>("""{"a":13,"b":37}""")
```

## Field names

Fields are serialised under their **Rust** name (`@SerialName("user_id")`). When decoding, the
Kotlin name (`userId`) is accepted too (`@JsonNames`). JSON produced by Rust's `serde` with
default settings can be read as is.

## What gets annotated

|         | Annotated                                                          |
| ------- | ------------------------------------------------------------------ |
| Records | always, unless listed in `skip_serializer_for`                     |
| Enums   | unless they contain objects or are listed in `skip_serializer_for` |

A record whose fields can't be serialised, for example because it contains an object or a custom
type mapped to a non-serialisable Kotlin class, fails to compile. Exclude it by name:

```toml
generate_serializable_records = true
skip_serializer_for = ["Session", "Cursor"]
```

## Fixtures

- [`tests/uniffi/serialization`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/serialization)
