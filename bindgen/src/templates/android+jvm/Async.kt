{% include "ffi/Async.kt" %}

object uniffiRustFutureContinuationCallbackCallback: UniffiRustFutureContinuationCallback {
    override fun callback(handle: Long, pollResult: Byte) {
        uniffiContinuationHandleMap.remove(handle).resume(pollResult)
    }
}

{%- if ci.has_async_callback_interface_definition() %}

object uniffiForeignFutureFreeImpl: UniffiForeignFutureFree {
    override fun callback(handle: Long) {
        val job = uniffiForeignFutureHandleMap.remove(handle)
        if (!job.isCompleted) {
            job.cancel()
        }
    }
}

{%- endif %}