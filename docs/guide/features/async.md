# Async

## Rust `async fn` → Kotlin `suspend fun`

Exported `async` functions, methods and constructors become `suspend` functions:

```rust
#[uniffi::export]
pub async fn say_after(ms: u16, who: String) -> String {
    sleep(Duration::from_millis(ms.into())).await;
    format!("Hello, {who}!")
}
```

```kotlin
suspend fun sayAfter(ms: UShort, who: String): String

runBlocking {
    sayAfter(200u, "Alice")   // "Hello, Alice!"
}
```

Errors work exactly as for synchronous functions: a `Result` turns into a thrown exception.

- **No executor needed on the Rust side.** The Kotlin side drives the future: it polls, and Rust
  wakes it up when there is progress. Plain `async` Rust code works as is.
- **Tokio.** Code that needs a Tokio runtime (for example `tokio::time::sleep`) must say so with
  `#[uniffi::export(async_runtime = "tokio")]`, and the crate must enable UniFFI's `tokio` feature.
- **Threading.** The poll loop runs on `Dispatchers.IO`, so calling a `suspend` binding from the
  main thread is safe.
- **Cancellation.** Cancelling the coroutine cancels the Rust future, which is then dropped. Any
  Rust code after the current `.await` doesn't run.

## Async callbacks

Methods of [callback and trait interfaces](callbacks.md) can be `async` too. The Kotlin
implementation is a `suspend fun`, and Rust `.await`s it:

```rust
#[uniffi::export(with_foreign)]
#[async_trait::async_trait]
pub trait AsyncParser: Send + Sync {
    async fn parse(&self, input: String) -> Result<i32, ParserError>;
}
```

```kotlin
class SlowParser : AsyncParser {
    override suspend fun parse(input: String): Int {
        delay(100)
        return input.toInt()
    }
}
```

The Kotlin coroutine is started in `GlobalScope`, because its real parent is a Rust future that
Kotlin's structured concurrency doesn't know about. If Rust drops the future before the coroutine
finishes, the coroutine is cancelled.

## Restrictions

- An `async` primary constructor (`new`) produces no Kotlin constructor. Name it differently so it
  becomes a `suspend fun` on the companion object.
- Borrowed byte slices (`&[u8]`) can't be arguments of `async` functions, because the borrow would
  end before the future does. The generator rejects them. Use `Vec<u8>` instead.

## Fixtures

- [`tests/uniffi/futures`](https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings/tree/main/tests/uniffi/futures)
