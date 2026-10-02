# Docstrings

Documentation comments on exported items are copied into the generated Kotlin as KDoc, so they
appear in the IDE for Kotlin users.

```rust
/// A person known to the address book.
#[derive(uniffi::Record)]
pub struct Person {
    /// Full name, as entered by the user.
    pub name: String,
}

/// Looks up a person by name.
#[uniffi::export]
pub fn find(name: String) -> Option<Person> { /* ... */ }
```

```kotlin
/**
 * A person known to the address book.
 */
data class Person(
    /**
     * Full name, as entered by the user.
     */
    var name: String,
)

/**
 * Looks up a person by name.
 */
fun find(name: String): Person?
```

This works for functions, objects, constructors, methods, records, fields, enums, variants, errors,
callback interfaces, and the namespace itself. In UDL, use `///` comments in the `.udl` file.

## Fixtures

- [`tests/uniffi/docstring`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/docstring) (UDL)
- [`tests/uniffi/docstring-proc-macro`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/docstring-proc-macro)
