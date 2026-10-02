# Builtin types

| Rust | UDL | Kotlin |
| --- | --- | --- |
| `bool` | `boolean` | `Boolean` |
| `i8` / `i16` / `i32` / `i64` | `i8` / `i16` / `i32` / `i64` | `Byte` / `Short` / `Int` / `Long` |
| `u8` / `u16` / `u32` / `u64` | `u8` / `u16` / `u32` / `u64` | `UByte` / `UShort` / `UInt` / `ULong` |
| `f32` / `f64` | `float` / `double` | `Float` / `Double` |
| `String` | `string` | `String` |
| `Vec<u8>` | `bytes` | `ByteArray` |
| `&[u8]` (arguments only) | `[ByRef] bytes` | `ByteArray` |
| `std::time::SystemTime` | `timestamp` | `kotlin.time.Instant` |
| `std::time::Duration` | `duration` | `kotlin.time.Duration` |
| `Option<T>` | `T?` | `T?` |
| `Vec<T>` | `sequence<T>` | `List<T>` |
| `HashMap<K, V>` | `record<K, V>` | `Map<K, V>` |
| `HashSet<T>` | — | `Set<T>` |

These types are copied when they cross the FFI. Unsigned integers map to Kotlin's unsigned types,
so the full range is preserved.

`Instant` and `Duration` are the standard library types from `kotlin.time`. There is no dependency
on `kotlinx-datetime`. Generated files opt into `kotlin.time.ExperimentalTime`.

## Borrowed bytes

A `&[u8]` argument lets Rust read a Kotlin `ByteArray` without copying it into a Rust-owned
buffer first:

```rust
#[uniffi::export]
pub fn sum_bytes(data: &[u8]) -> u64 {
    data.iter().map(|b| *b as u64).sum()
}
```

```kotlin
sumBytes(byteArrayOf(1, 2, 3))   // 6u
```

The slice is only valid during the call, so Rust can't keep it. On Kotlin/Native the array is
pinned for the duration of the call. On JVM and Android it is copied once into native memory.

Borrowed bytes work in functions, methods and constructors, but not in `async` functions, and not
as return values or callback arguments.

## Fixtures

- [`tests/uniffi/coverall`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/coverall)
- [`tests/uniffi/chronological`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/chronological)
- [`tests/uniffi/type-limits`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/type-limits)
- [`tests/uniffi/borrowed-bytes`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/borrowed-bytes)
