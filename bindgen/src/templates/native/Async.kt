// Async return type handlers
actual fun createUniffiRustFutureContinuationCallbackCallback() : Any = staticCFunction { handle: Long, pollResult: Byte ->
    uniffiContinuationHandleMap.remove(handle).resume(pollResult)
}
