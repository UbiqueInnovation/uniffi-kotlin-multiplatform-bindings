@file:OptIn(ExperimentalForeignApi::class)

package uniffi.runtime

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

const val UNIFFI_RUST_FUTURE_POLL_READY = 0.toByte()
const val UNIFFI_RUST_FUTURE_POLL_MAYBE_READY = 1.toByte()

val uniffiContinuationHandleMap = UniffiHandleMap<CancellableContinuation<Byte>>()

val UniffiRustFutureContinuationCallbackCallback = staticCFunction { handle: Long, pollResult: Byte ->
    uniffiContinuationHandleMap.remove(handle).resume(pollResult)
}

// FFI type for Rust future continuations
suspend fun<T, F, E: Exception> uniffiRustCallAsync(
    rustFuture: Long,
    pollFunc: (Long, Any, Long) -> Unit,
    completeFunc: (Long, UniffiRustCallStatus) -> F,
    freeFunc: (Long) -> Unit,
    cancelFunc: (Long) -> Unit,
    liftFunc: (F) -> T,
    errorHandler: UniffiRustCallStatusErrorHandler<E>
): T {
    return withContext(Dispatchers.IO) {
        try {
            do {
                val pollResult = suspendCancellableCoroutine<Byte> { continuation ->
                    val handle = uniffiContinuationHandleMap.insert(continuation)
                    continuation.invokeOnCancellation {
                        cancelFunc(rustFuture)
                    }
                    pollFunc(
                        rustFuture,
                        UniffiRustFutureContinuationCallbackCallback,
                        handle
                    )
                }
            } while (pollResult != UNIFFI_RUST_FUTURE_POLL_READY);

            return@withContext liftFunc(
                uniffiRustCallWithError(errorHandler, { status -> completeFunc(rustFuture, status) })
            )
        } finally {
            freeFunc(rustFuture)
        }
    }
}

val UniffiForeignFutureFreeImpl = staticCFunction { handle: Long ->
    val job = uniffiForeignFutureHandleMap.remove(handle)
    if (!job.isCompleted) {
        job.cancel()
    }
}

inline fun<T> uniffiTraitInterfaceCallAsync(
    crossinline makeCall: suspend () -> T,
    crossinline handleSuccess: (T) -> Unit,
    crossinline handleError: (UniffiRustCallStatusByValue) -> Unit,
): UniffiForeignFutureUniffiByValue {
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
            val status = UniffiRustCallStatusByValue(
                UNIFFI_CALL_UNEXPECTED_ERROR,
                FfiConverterString.lower(e.toString()),
            )
            handleError(status)
        }
    }
    val handle = uniffiForeignFutureHandleMap.insert(job)
    return UniffiForeignFutureUniffiByValue(handle, UniffiForeignFutureFreeImpl)
}

inline fun<T, reified E: Throwable> uniffiTraitInterfaceCallAsyncWithError(
    crossinline makeCall: suspend () -> T,
    crossinline handleSuccess: (T) -> Unit,
    crossinline handleError: (UniffiRustCallStatusByValue) -> Unit,
    crossinline lowerError: (E) -> RustBufferByValue,
): UniffiForeignFutureUniffiByValue {
    // See uniffiTraitInterfaceCallAsync for details on `DelicateCoroutinesApi`
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch {
        try {
            handleSuccess(makeCall())
        } catch(e: Exception) {
            if (e is E) {
                val status = UniffiRustCallStatusByValue(
                    UNIFFI_CALL_ERROR,
                    lowerError(e),
                )
                handleError(status)
            } else {
                val status = UniffiRustCallStatusByValue(
                    UNIFFI_CALL_UNEXPECTED_ERROR,
                    FfiConverterString.lower(e.toString()),
                )
                handleError(status)
            }
        }
    }
    val handle = uniffiForeignFutureHandleMap.insert(job)
    return UniffiForeignFutureUniffiByValue(handle, UniffiForeignFutureFreeImpl)
}

val uniffiForeignFutureHandleMap = UniffiHandleMap<Job>()
