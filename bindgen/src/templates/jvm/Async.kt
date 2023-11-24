internal actual typealias UniFfiRustFutureContinuationCallbackType = UniFfiRustFutureContinuationCallbackImpl

internal actual fun createUniFfiRustFutureContinuationCallback(): UniFfiRustFutureContinuationCallbackType =
    UniFfiRustFutureContinuationCallbackImpl()

internal class UniFfiRustFutureContinuationCallbackImpl : com.sun.jna.Callback {
    fun invoke(continuationHandle: kotlin.ULong, pollResult: kotlin.Short) = resumeContinuation(continuationHandle, pollResult)
}
