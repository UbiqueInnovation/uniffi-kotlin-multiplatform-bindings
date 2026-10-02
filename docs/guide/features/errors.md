# Errors

A Rust function that returns `Result<T, E>` throws in Kotlin. `E` must be an exported error type.

## Error enums

```rust
#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum ArithmeticError {
    #[error("Integer overflow on {a} + {b}")]
    IntegerOverflow { a: u64, b: u64 },
    #[error("Division by zero")]
    DivisionByZero,
}

#[uniffi::export]
pub fn add(a: u64, b: u64) -> Result<u64, ArithmeticError> {
    a.checked_add(b).ok_or(ArithmeticError::IntegerOverflow { a, b })
}
```

```kotlin
sealed class ArithmeticException : kotlin.Exception() {
    class IntegerOverflow(val a: ULong, val b: ULong) : ArithmeticException()
    class DivisionByZero : ArithmeticException()
}

try {
    add(ULong.MAX_VALUE, 1u)
} catch (e: ArithmeticException.IntegerOverflow) {
    println("${e.a} + ${e.b} overflows")
}
```

- An error type is always a `sealed class` extending `kotlin.Exception`, with one subclass per
  variant.
- A trailing `Error` in the name becomes `Exception`: `ArithmeticError` turns into
  `ArithmeticException`. This applies to error enums and to [objects used as errors](#objects-as-errors).
- For variants with fields, `message` lists the fields (`a=1, b=2`).

### Flat errors

If only the error message matters, mark the enum `flat_error`. The variants lose their fields and
carry the Rust `Display` output as `message`:

```rust
#[derive(Debug, thiserror::Error, uniffi::Error)]
#[uniffi(flat_error)]
pub enum NetworkError {
    #[error("timed out after {0:?}")]
    Timeout(std::time::Duration),
    #[error("connection refused")]
    Refused,
}
```

```kotlin
sealed class NetworkException(message: String) : kotlin.Exception(message) {
    class Timeout(message: String) : NetworkException(message)
    class Refused(message: String) : NetworkException(message)
}
```

This is useful when a variant holds something that can't cross the FFI.

## Objects as errors

An [object](functions-and-objects.md) can be an error too. This is handy for wrapping
`anyhow::Error` and keeping methods on it:

```rust
#[derive(Debug, thiserror::Error, uniffi::Object)]
#[error("{e:?}")]
pub struct RichError {
    e: anyhow::Error,
}

#[uniffi::export]
impl RichError {
    fn chain(&self) -> Vec<String> {
        self.e.chain().map(ToString::to_string).collect()
    }
}

#[uniffi::export]
fn oops() -> Result<(), Arc<RichError>> { /* ... */ }
```

The generated class extends `kotlin.Exception`, so you can catch it and still call `chain()`:

```kotlin
try {
    oops()
} catch (e: RichException) {
    e.chain()
}
```

## `@Throws` and Java callers

Throwing functions and methods are annotated with `@Throws(SomeException::class)`. On the JVM this
puts a `throws` clause into the class file, so Java callers can catch the exception. Throwing
`suspend` functions also declare `CancellationException`.

## Panics and unexpected errors

A Rust panic does not crash the process. It surfaces in Kotlin as `InternalException` with the panic
message. The class lives in `uniffi.runtime`, and the bindings' package has a typealias for it. The same exception is thrown for other failures that are not part of the
declared error type.

## Fixtures

- [`tests/uniffi/error-types`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/error-types)
- [`tests/uniffi/interface-throws`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/interface-throws)
- [`tests/uniffi/coverall`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/coverall)
