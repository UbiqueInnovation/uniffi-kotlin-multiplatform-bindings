# Callbacks and trait interfaces

Rust can call code that is implemented in Kotlin. You declare a Rust trait, implement it in Kotlin,
and pass the implementation to a Rust function.

There are two flavours:

| Rust | Kotlin | Who implements it |
| --- | --- | --- |
| `#[uniffi::export(callback_interface)]` trait, or `callback interface` in UDL | `interface Foo` | Kotlin only |
| `#[uniffi::export(with_foreign)]` trait, or `[Trait, WithForeign]` in UDL | `interface Foo` and `class FooImpl` | Rust or Kotlin |

A plain `#[uniffi::export]` trait can only be implemented in Rust, and behaves like an
[object](functions-and-objects.md).

## Callback interfaces

```rust
#[uniffi::export(callback_interface)]
pub trait Logger: Send + Sync {
    fn log(&self, message: String);
}

#[uniffi::export]
pub fn run_job(logger: Box<dyn Logger>) {
    logger.log("starting".into());
    // ...
}
```

```kotlin
class PrintLogger : Logger {
    override fun log(message: String) = println(message)
}

runJob(PrintLogger())
```

Callback interfaces are passed as `Box<dyn Trait>` and can only travel from Kotlin to Rust.

## Trait interfaces with foreign implementations

A `with_foreign` trait can be implemented on both sides, and can travel in both directions.
Rust receives it as `Arc<dyn Trait>`:

```rust
#[uniffi::export(with_foreign)]
pub trait Greeter: Send + Sync {
    fn greet(&self, name: String) -> String;
}

#[uniffi::export]
pub fn greet_everyone(greeter: Arc<dyn Greeter>, names: Vec<String>) -> Vec<String> {
    names.into_iter().map(|n| greeter.greet(n)).collect()
}

#[uniffi::export]
pub fn default_greeter() -> Arc<dyn Greeter> {
    Arc::new(EnglishGreeter)
}
```

```kotlin
// A Kotlin implementation, passed to Rust
class Pirate : Greeter {
    override fun greet(name: String) = "Ahoy, $name!"
}
greetEveryone(Pirate(), listOf("Ann", "Bob"))

// A Rust implementation, used from Kotlin. Its type is GreeterImpl.
val greeter: Greeter = defaultGreeter()
greeter.greet("Ann")
```

When a Kotlin implementation goes to Rust and comes back, you get the same Kotlin object, not a
wrapper.

## Errors

Callback methods can throw the error types declared in the trait signature:

```rust
#[uniffi::export(callback_interface)]
pub trait Fetcher: Send + Sync {
    fn fetch(&self, url: String) -> Result<String, FetchError>;
}
```

```kotlin
class KtorFetcher : Fetcher {
    override fun fetch(url: String): String {
        throw FetchException.NotFound()   // arrives in Rust as Err(FetchError::NotFound)
    }
}
```

Any other exception thrown by the Kotlin code is reported to Rust as an *unexpected* error. For Rust
to accept that, the error type needs a conversion:

```rust
impl From<uniffi::UnexpectedUniFFICallbackError> for FetchError {
    fn from(e: uniffi::UnexpectedUniFFICallbackError) -> Self {
        FetchError::Unexpected { reason: e.reason }
    }
}
```

If a method has no error type, an unexpected exception makes the Rust side panic.

## Async callbacks

Trait methods can be `async`. The Kotlin implementation is then a `suspend fun`, and Rust awaits it.
See [Async](async.md#async-callbacks).

## Lifetime

The Kotlin object is kept alive as long as Rust holds a reference to it. When Rust drops the last
`Box` or `Arc`, the Kotlin side releases it. You don't need to do anything to manage this.

!!! warning "Multi-module projects"
    A Kotlin implementation of a trait declared in one module's crate can't be passed to a
    function of a *different* module. See [Multi-module projects](multi-module.md#limitations).

## Fixtures

- [`tests/uniffi/callbacks`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/callbacks)
- [`tests/uniffi/coverall`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/coverall)
- [`tests/uniffi/futures`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/futures)
