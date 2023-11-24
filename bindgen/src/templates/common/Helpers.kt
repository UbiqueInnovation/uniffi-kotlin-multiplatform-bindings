// A handful of classes and functions to support the generated data structures.
// This would be a good candidate for isolating in its own ffi-support lib.
// Error runtime.
// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class RustCallStatus
internal expect val RustCallStatus.statusCode: kotlin.Byte
internal expect val RustCallStatus.errorBuffer: RustBuffer

internal expect fun <T> withRustCallStatus(block: (RustCallStatus) -> T): T

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class RustCallStatusByValue

private const val RUST_CALL_STATUS_SUCCESS: kotlin.Byte = 0
private const val RUST_CALL_STATUS_ERROR: kotlin.Byte = 1
private const val RUST_CALL_STATUS_PANIC: kotlin.Byte = 2

internal fun RustCallStatus.isSuccess(): kotlin.Boolean {
    return statusCode == RUST_CALL_STATUS_SUCCESS
}

internal fun RustCallStatus.isError(): kotlin.Boolean {
    return statusCode == RUST_CALL_STATUS_ERROR
}

internal fun RustCallStatus.isPanic(): kotlin.Boolean {
    return statusCode == RUST_CALL_STATUS_PANIC
}

class InternalException(message: kotlin.String) : Exception(message)

// Each top-level error class has a companion object that can lift the error from the call status's rust buffer
internal interface CallStatusErrorHandler<E> {
    fun lift(errorBuffer: RustBuffer): E;
}

// Helpers for calling Rust
// In practice we usually need to be synchronized to call this safely, so it doesn't
// synchronize itself

// Call a rust function that returns a Result<>.  Pass in the Error class companion that corresponds to the Err
internal inline fun <U, E : Exception> rustCallWithError(
    errorHandler: CallStatusErrorHandler<E>,
    crossinline callback: (RustCallStatus) -> U,
): U =
    withRustCallStatus { status: RustCallStatus ->
        val return_value = callback(status)
        checkCallStatus(errorHandler, status)
        return_value
    }

// Check RustCallStatus and throw an error if the call wasn't successful
internal fun <E : Exception> checkCallStatus(errorHandler: CallStatusErrorHandler<E>, status: RustCallStatus) {
    if (status.isSuccess()) {
        return
    } else if (status.isError()) {
        throw errorHandler.lift(status.errorBuffer)
    } else if (status.isPanic()) {
        // when the rust code sees a panic, it tries to construct a rustbuffer
        // with the message.  but if that code panics, then it just sends back
        // an empty buffer.
        if (status.errorBuffer.dataSize > 0) {
            // TODO avoid additional copy
            throw InternalException(FfiConverterString.lift(status.errorBuffer))
        } else {
            throw InternalException("Rust panic")
        }
    } else {
        throw InternalException("Unknown rust call status: $status.code")
    }
}

// CallStatusErrorHandler implementation for times when we don't expect a CALL_ERROR
internal object NullCallStatusErrorHandler : CallStatusErrorHandler<InternalException> {
    override fun lift(errorBuffer: RustBuffer): InternalException {
        errorBuffer.free()
        return InternalException("Unexpected CALL_ERROR")
    }
}

// Call a rust function that returns a plain value
internal inline fun <U> rustCall(crossinline callback: (RustCallStatus) -> U): U {
    return rustCallWithError(NullCallStatusErrorHandler, callback);
}

// Map handles to objects
//
// This is used when the Rust code expects an opaque pointer to represent some foreign object.
// Normally we would pass a pointer to the object, but JNA doesn't support getting a pointer from an
// object reference , nor does it support leaking a reference to Rust.
//
// Instead, this class maps ULong values to objects so that we can pass a pointer-sized type to
// Rust when it needs an opaque pointer.
//
// TODO: refactor callbacks to use this class
internal expect class UniFfiHandleMap<T : Any>() {

    val size: kotlin.Int

    fun insert(obj: T): kotlin.ULong

    fun get(handle: kotlin.ULong): T?

    fun remove(handle: kotlin.ULong): T?
}
