# UniFFI traits

Some Rust standard traits can be exported, and are mapped onto the matching Kotlin members. This
works on objects, records and enums.

| Rust trait | Kotlin |
| --- | --- |
| `Display` | `toString()` |
| `Debug` | `toString()`, if there is no `Display` |
| `Eq` | `equals(other)` |
| `Hash` | `hashCode()` |
| `Ord` | `compareTo(other)`, and the class implements `Comparable` |

Each of these members calls into Rust, so the Rust implementation decides the result.

## Exporting

With proc-macros, list the traits in `#[uniffi::export(...)]` on the type. The type must implement
them:

```rust
#[derive(Debug, PartialEq, Eq, PartialOrd, Ord, Hash, uniffi::Object)]
#[uniffi::export(Debug, Display, Eq, Hash, Ord)]
pub struct Version {
    major: u32,
    minor: u32,
}

impl std::fmt::Display for Version {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}.{}", self.major, self.minor)
    }
}
```

In UDL, use the `Traits` attribute:

```webidl
[Traits=(Display, Debug, Eq, Hash, Ord)]
interface Version {
    constructor(u32 major, u32 minor);
};
```

```kotlin
val a = Version(1u, 2u)
val b = Version(1u, 10u)
"$a"                         // "1.2"
a == Version(1u, 2u)         // true, compared by Rust
listOf(b, a).sorted()        // [1.2, 1.10]
```

## Records and enums

Records and enums are `data class`es, which already have `equals`, `hashCode` and `toString`.
Exporting the traits replaces those with the Rust implementations.

Fieldless enums become Kotlin `enum class`es, where `equals`, `hashCode` and `compareTo` are final.
For them only `toString` is generated. Kotlin's built-in behaviour matches a derived Rust
implementation, except that `compareTo` uses declaration order, while Rust's derived `Ord` uses
explicit discriminants if there are any.

## Fixtures

- [`tests/uniffi/trait-methods`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/trait-methods)
