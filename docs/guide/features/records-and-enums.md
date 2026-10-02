# Records and enums

Records and enums are passed **by value**: every time one crosses the FFI it is serialised into a
buffer and rebuilt on the other side. Changing a record in Kotlin does not change anything in Rust.

## Records

```rust
#[derive(uniffi::Record)]
pub struct Person {
    pub name: String,
    pub age: u32,
    pub email: Option<String>,
}
```

```kotlin
data class Person(
    var name: String,
    var age: UInt,
    var email: String?,
)
```

Records become `data class`es, so you get `copy`, `equals`, `hashCode` and destructuring.

### Default values

Field defaults become constructor defaults:

```rust
#[derive(uniffi::Record)]
pub struct Settings {
    pub name: String,
    #[uniffi(default = 42)]
    pub retries: i32,
    #[uniffi(default = [])]
    pub tags: Vec<String>,
    #[uniffi(default = None)]
    pub proxy: Option<String>,
    #[uniffi(default)]          // the type's own default: 0, "", false, empty list, ...
    pub verbose: bool,
}
```

```kotlin
data class Settings(
    var name: String,
    var retries: Int = 42,
    var tags: List<String> = listOf(),
    var proxy: String? = null,
    var verbose: Boolean = false,
)
```

### Immutable records

Record fields are `var` by default. To generate `val` fields instead, set this in `uniffi.toml`:

```toml
generate_immutable_records = true
```

Individual records can keep their `var` fields:

```toml
generate_immutable_records = true
mutable_records = ["Cursor", "Draft"]
```

`mutable_records` uses the record name as declared in Rust or UDL. Names that don't match a record
are ignored.

## Enums

A Rust enum without fields becomes a Kotlin `enum class`:

```rust
#[derive(uniffi::Enum)]
pub enum Animal {
    Dog,
    Cat,
}
```

```kotlin
enum class Animal {
    DOG,
    CAT;
}
```

Explicit discriminants are kept and exposed as `value`:

```rust
#[derive(uniffi::Enum)]
#[repr(u64)]
pub enum AnimalLargeUInt {
    Dog = 4294967298,
    Cat = 4294967299,
}
```

```kotlin
enum class AnimalLargeUInt(val value: ULong) {
    DOG(4294967298u),
    CAT(4294967299u);
}
```

An enum with fields becomes a `sealed class` with one subclass per variant. Variants without
fields are `object`s, the others `data class`es:

```rust
#[derive(uniffi::Enum)]
pub enum Shape {
    Empty,
    Circle { radius: f64 },
    Rectangle(f64, f64),
}
```

```kotlin
sealed class Shape {
    object Empty : Shape()
    data class Circle(val radius: Double) : Shape()
    data class Rectangle(val v1: Double, val v2: Double) : Shape()
}

when (shape) {
    Shape.Empty -> 0.0
    is Shape.Circle -> PI * shape.radius * shape.radius
    is Shape.Rectangle -> shape.v1 * shape.v2
}
```

Unnamed fields are called `v1`, `v2`, and so on. Variant fields are always `val`.

Variant fields can have defaults with `#[uniffi(default = ...)]`, just like record fields.

## Methods on records and enums

Records and enums can have exported methods. In Kotlin they are ordinary member functions:

```rust
#[derive(uniffi::Record)]
pub struct Greeting {
    pub text: String,
}

#[uniffi::export]
impl Greeting {
    fn shout(&self) -> String {
        self.text.to_uppercase()
    }

    async fn shout_later(&self) -> String {
        self.text.to_uppercase()
    }
}
```

```kotlin
val greeting = Greeting("hello")
greeting.shout()          // "HELLO"
greeting.shoutLater()     // suspend
```

Each call serialises the receiver and sends the copy to Rust, so a method can't change the Kotlin
value it was called on. To derive `toString`, `equals`, `hashCode` and `compareTo` from Rust trait
implementations, see [UniFFI traits](uniffi-traits.md).

## Records and enums that hold objects

When a record or enum contains an [object](functions-and-objects.md), the generated class also
implements `Disposable`, and its `destroy()` releases the objects it holds.

## Fixtures

- [`tests/uniffi/enum-types`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/enum-types)
- [`tests/uniffi/struct-default-values`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/struct-default-values)
- [`tests/uniffi/defaults`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/defaults)
- [`tests/uniffi/mutable-records`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/mutable-records)
- [`tests/uniffi/trait-methods`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/trait-methods)
