# Functions and objects

## Functions

Every exported function becomes a top-level Kotlin function in the bindings' package. Names are
converted to `lowerCamelCase`.

```rust
#[uniffi::export]
pub fn greet(name: String) -> String {
    format!("Hello, {name}!")
}

#[uniffi::export(default(count = 3))]
pub fn repeat(text: String, count: u32) -> String {
    text.repeat(count as usize)
}
```

```kotlin
greet("Kotlin")        // "Hello, Kotlin!"
repeat("ab")           // "ababab", count defaults to 3
```

Default values declared on the Rust side become Kotlin default arguments. `default(max_splits)`
without a value uses the type's own default (`None` for an `Option`). The same works on
`#[uniffi::constructor(default(...))]` and `#[uniffi::method(default(...))]`.

## Objects

An object is a Rust struct that lives on the Rust side. Kotlin holds a reference to it and calls
its methods across the FFI. Unlike [records](records-and-enums.md), it is never copied.

```rust
use std::sync::atomic::{AtomicI32, Ordering};
use std::sync::Arc;

#[derive(uniffi::Object)]
pub struct Counter {
    value: AtomicI32,
}

#[uniffi::export]
impl Counter {
    #[uniffi::constructor]
    pub fn new(start: i32) -> Self {
        Self { value: AtomicI32::new(start) }
    }

    #[uniffi::constructor]
    pub fn from_string(text: String) -> Arc<Self> {
        Arc::new(Self::new(text.parse().unwrap_or(0)))
    }

    pub fn increment(&self) -> i32 {
        self.value.fetch_add(1, Ordering::SeqCst) + 1
    }
}
```

The generator produces an interface with the methods, and a class implementing it:

```kotlin
interface CounterInterface {
    fun increment(): Int
}

open class Counter : Disposable, CounterInterface {
    constructor(start: Int)            // the constructor named `new`
    companion object {
        fun fromString(text: String): Counter   // every other constructor
    }
    override fun increment(): Int
    override fun destroy()
    override fun close()
}
```

```kotlin
val counter = Counter(41)
counter.increment()  // 42
```

- The constructor named `new` becomes the Kotlin constructor. `#[uniffi::constructor(name = "new")]`
  makes any constructor the primary one.
- All other constructors become functions on the `companion object`.
- An `async` primary constructor can't be a Kotlin constructor, so no constructor is generated for
  it. Use a named async constructor instead, which becomes a `suspend fun` on the companion.
- Methods must take `&self` or `self: Arc<Self>`. The object is shared, so it must be `Send + Sync`
  and use interior mutability where it changes.

Objects can be passed to and returned from functions, and stored in records, lists, maps and
optionals.

## Lifetime

Each Kotlin object holds a reference to the Rust object. The reference is released either
explicitly or when the Kotlin object is garbage collected.

```kotlin
// Explicitly
counter.destroy()

// Scoped, like Closeable.use
Counter(0).use { c ->
    c.increment()
}
```

Generated objects implement `Disposable`, which extends `AutoCloseable`. `close()` and `destroy()`
do the same thing.

- After `destroy()`, every method call throws `IllegalStateException`. `destroy()` itself can
  be called any number of times.
- A call that is in progress when `destroy()` is called completes normally. The Rust object is
  freed when the last running call finishes.
- If you never call `destroy()`, a cleaner frees the Rust object some time after the Kotlin object
  becomes unreachable. That is fine for small objects. For objects holding significant resources
  (file handles, large buffers), release them explicitly.
- Records and enums that contain objects also implement `Disposable`, and their `destroy()`
  destroys the objects they hold.

## Interfaces and naming

| Rust declaration | Kotlin interface | Kotlin class |
| --- | --- | --- |
| `#[derive(uniffi::Object)] struct Foo` | `FooInterface` | `Foo` |
| `#[uniffi::export] trait Foo` (Rust-only trait) | `FooInterface` | `Foo` |
| `#[uniffi::export(with_foreign)] trait Foo` | `Foo` | `FooImpl` |

For the last row, users are expected to implement `Foo` in Kotlin, so the interface gets the
short name. See [Callbacks and trait interfaces](callbacks.md).

## Fakes in tests

Every object has a constructor that takes `NoHandle`. It creates an instance without a Rust object
behind it, which is useful as a base for test fakes:

```kotlin
class FakeCounter : Counter(NoHandle) {
    override fun increment(): Int = 7
}
```

Any call that reaches the Rust side through such an instance fails.

## Fixtures

- [`tests/uniffi/simple-fns`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/simple-fns)
- [`tests/uniffi/simple-iface`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/simple-iface)
- [`tests/uniffi/proc-macro`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/proc-macro)
- [`tests/uniffi/defaults`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/defaults)
- [`tests/uniffi/coverall`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/coverall)
