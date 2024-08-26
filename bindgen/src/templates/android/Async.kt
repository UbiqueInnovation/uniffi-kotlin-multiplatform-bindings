// Async return type handlers
actual fun createUniffiRustFutureContinuationCallbackCallback() : Any {
    return object: Callback {
        fun callback(handle: Long, pollResult: Byte) {
            uniffiContinuationHandleMap.remove(handle).resume(pollResult)
        }
    }
}
