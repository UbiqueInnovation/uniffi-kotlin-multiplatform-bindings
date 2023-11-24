// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
internal actual typealias UniFfiRustFutureContinuationCallbackType = CPointer<CFunction<(kotlin.ULong, kotlin.Short) -> Unit>>

internal actual fun createUniFfiRustFutureContinuationCallback(): UniFfiRustFutureContinuationCallbackType =
    staticCFunction<kotlin.ULong, kotlin.Short, Unit> { continuationHandle: kotlin.ULong, pollResult: kotlin.Short ->
        resumeContinuation(continuationHandle, pollResult)
    }
