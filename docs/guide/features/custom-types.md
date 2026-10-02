# Custom types

A custom type is a Rust type that crosses the FFI as one of the [builtin types](builtin-types.md).
You tell UniFFI how to convert between the two. By default, Kotlin only sees the builtin type.
You can also configure your own Kotlin type for it.

## Rust side

For a newtype, `custom_newtype!` is enough:

```rust
pub struct UserId(pub i64);
uniffi::custom_newtype!(UserId, i64);
```

For anything else, use `custom_type!` with conversion functions. If the type comes from another
crate, add `remote`:

```rust
use url::Url;

uniffi::custom_type!(Url, String, {
    remote,                                // Url is defined in the `url` crate
    lower: |url| url.into(),
    try_lift: |s| Ok(Url::parse(&s)?),
});
```

In UDL, declare custom types with `[Custom] typedef string Url;`.

## Kotlin side: default

Without configuration, the custom type is a `typealias` to the builtin type:

```kotlin
typealias UserId = Long
typealias Url = String
```

## Kotlin side: your own type

To use a real Kotlin type, configure it in `uniffi.toml`:

```toml
[custom_types.Url]
type_name = "io.ktor.http.Url"          # the Kotlin type
imports = ["io.ktor.http.Url"]          # imports the conversions need
lift = "Url({})"                        # builtin → Kotlin
lower = "{}.toString()"                 # Kotlin → builtin
```

`{}` is replaced with the value being converted. `into_custom` and `from_custom` are accepted as older
names for `lift` and `lower`.

```kotlin
val demo = getCustomTypesDemo(null)
demo.url == Url("http://example.com/")   // a real io.ktor.http.Url
```

The Kotlin type has to be available in `commonMain`. Add its library as a dependency. Default
values of fields with a custom type are converted with the `lift` expression too.

## Fixtures

- [`examples/custom-types`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/examples/custom-types)
- [`tests/uniffi/defaults`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/defaults)
- [`tests/uniffi/ext-types/custom-types`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/ext-types/custom-types)
