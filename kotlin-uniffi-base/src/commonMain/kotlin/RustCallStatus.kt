// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class RustCallStatus

internal const val RUST_CALL_STATUS_SUCCESS : Byte = 0
internal const val RUST_CALL_STATUS_ERROR : Byte = 1
internal const val RUST_CALL_STATUS_PANIC : Byte = 2

fun RustCallStatus.isSuccess(): Boolean = statusCode == RUST_CALL_STATUS_SUCCESS

fun RustCallStatus.isError(): Boolean = statusCode == RUST_CALL_STATUS_ERROR

fun RustCallStatus.isPanic(): Boolean = statusCode == RUST_CALL_STATUS_PANIC

expect val RustCallStatus.statusCode: Byte

expect val RustCallStatus.errorBuffer: RustBuffer

expect fun <T> withRustCallStatus(block: (RustCallStatus) -> T): T

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class RustCallStatusByValue
