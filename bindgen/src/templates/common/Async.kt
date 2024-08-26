// Async return type handlers

internal const val UNIFFI_RUST_FUTURE_POLL_READY = 0.toByte()
internal const val UNIFFI_RUST_FUTURE_POLL_MAYBE_READY = 1.toByte()

internal val uniffiContinuationHandleMap = UniffiHandleMap<CancellableContinuation<Byte>>()

expect fun createUniffiRustFutureContinuationCallbackCallback() : Any

// FFI type for Rust future continuations
internal suspend fun<T, F, E: kotlin.Exception> uniffiRustCallAsync(
    rustFuture: Long,
    pollFunc: (Long, Any, Long) -> Unit,
    completeFunc: (Long, UniffiRustCallStatus) -> F,
    freeFunc: (Long) -> Unit,
    cancelFunc: (Long) -> Unit,
    liftFunc: (F) -> T,
    errorHandler: UniffiRustCallStatusErrorHandler<E>
): T {
    return withContext(Dispatchers.IO) {
        val continuationCallback = createUniffiRustFutureContinuationCallbackCallback()
        try {
            do {
                val pollResult = suspendCancellableCoroutine<Byte> { continuation ->
                    val handle = uniffiContinuationHandleMap.insert(continuation)
                    continuation.invokeOnCancellation {
                        uniffiContinuationHandleMap.remove(handle)
                        cancelFunc(rustFuture)
                    }
                    pollFunc(
                        rustFuture,
                        continuationCallback,
                        handle
                    )
                }
            } while (pollResult != UNIFFI_RUST_FUTURE_POLL_READY);

            return@withContext liftFunc(
                uniffiRustCallWithError(errorHandler, { status -> completeFunc(rustFuture, status) })
            )
        } finally {
            println(continuationCallback)
            freeFunc(rustFuture)
        }
    }
}

{%- if ci.has_async_callback_interface_definition() %}
internal inline fun<T> uniffiTraitInterfaceCallAsync(
    crossinline makeCall: suspend () -> T,
    crossinline handleSuccess: (T) -> Unit,
    crossinline handleError: (UniffiRustCallStatusByValue) -> Unit,
): UniffiForeignFuture {
    // Using `GlobalScope` is labeled as a "delicate API" and generally discouraged in Kotlin programs, since it breaks structured concurrency.
    // However, our parent task is a Rust future, so we're going to need to break structure concurrency in any case.
    //
    // Uniffi does its best to support structured concurrency across the FFI.
    // If the Rust future is dropped, `uniffiForeignFutureFreeImpl` is called, which will cancel the Kotlin coroutine if it's still running.
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch {
        try {
            handleSuccess(makeCall())
        } catch(e: Exception) {
            val status = UniffiRustCallStatusHelper.allocValue()
            status.code = UNIFFI_CALL_UNEXPECTED_ERROR
            status.error_buf = {{ Type::String.borrow()|lower_fn }}(e.toString())
            handleError(status)
        }
    }
    val handle = uniffiForeignFutureHandleMap.insert(job)
    return UniffiForeignFuture(handle, createUniffiForeignFutureFreeCallback {
        handle: Long ->
        val job = uniffiForeignFutureHandleMap.remove(handle)
        if (!job.isCompleted) {
            job.cancel()
        }
    })
}

internal inline fun<T, reified E: Throwable> uniffiTraitInterfaceCallAsyncWithError(
    crossinline makeCall: suspend () -> T,
    crossinline handleSuccess: (T) -> Unit,
    crossinline handleError: (UniffiRustCallStatusByValue) -> Unit,
    crossinline lowerError: (E) -> RustBufferByValue,
): UniffiForeignFuture {
    // See uniffiTraitInterfaceCallAsync for details on `DelicateCoroutinesApi`
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch {
        try {
            handleSuccess(makeCall())
        } catch(e: Exception) {
            val status = UniffiRustCallStatusHelper.allocValue()
            if (e is E) {
                status.code = UNIFFI_CALL_ERROR
                status.error_buf = lowerError(e)
                handleError(status)
            } else {
                status.code = UNIFFI_CALL_UNEXPECTED_ERROR
                status.error_buf = {{ Type::String.borrow()|lower_fn }}(e.toString())
                handleError(status)
            }
        }
    }
    val handle = uniffiForeignFutureHandleMap.insert(job)
    return UniffiForeignFuture(handle, uniffiForeignFutureFreeImpl)
}

internal val uniffiForeignFutureHandleMap = UniffiHandleMap<Job>()
// For testing
public fun uniffiForeignFutureHandleCount() = uniffiForeignFutureHandleMap.size

{%- endif %}
