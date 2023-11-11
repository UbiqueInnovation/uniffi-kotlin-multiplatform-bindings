// Call a rust function that returns a Result<>.  Pass in the Error class companion that corresponds to the Err
internal inline fun <U, E: Exception> rustCallWithError(errorHandler: CallStatusErrorHandler<E>, crossinline callback: (RustCallStatus) -> U): U =
    withRustCallStatus { status ->
        val return_value = callback(status)
        checkCallStatus(errorHandler, status)
        return_value
    }

// Check RustCallStatus and throw an error if the call wasn't successful
internal fun<E: Exception> checkCallStatus(errorHandler: CallStatusErrorHandler<E>, status: RustCallStatus) {
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

interface CallStatusErrorHandler<E> {
    fun lift(errorBuffer: RustBuffer): E;
}

object NullCallStatusErrorHandler : CallStatusErrorHandler<InternalException> {
    override fun lift(errorBuffer: RustBuffer): InternalException {
        errorBuffer.free()
        return InternalException("Unexpected CALL_ERROR")
    }
}

internal inline fun <U> rustCall(crossinline callback: (RustCallStatus) -> U): U {
    return rustCallWithError(NullCallStatusErrorHandler, callback);
}
