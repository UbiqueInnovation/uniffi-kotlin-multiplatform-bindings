@com.sun.jna.Structure.FieldOrder("code", "error_buf")
internal actual open class RustCallStatus : com.sun.jna.Structure() {
    @JvmField var code: kotlin.Byte = 0
    @JvmField var error_buf: RustBuffer = RustBuffer()
}

internal actual val RustCallStatus.statusCode: kotlin.Byte
    get() = code
internal actual val RustCallStatus.errorBuffer: RustBuffer
    get() = error_buf

internal actual fun <T> withRustCallStatus(block: (RustCallStatus) -> T): T {
    val rustCallStatus = RustCallStatus()
    return block(rustCallStatus)
}

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
internal actual open class RustCallStatusByValue : RustCallStatus(), com.sun.jna.Structure.ByValue

internal actual class UniFfiHandleMap<T : Any> {
    private val map = java.util.concurrent.ConcurrentHashMap<kotlin.ULong, T>()

    // Use AtomicInteger for our counter, since we may be on a 32-bit system.  4 billion possible
    // values seems like enough. If somehow we generate 4 billion handles, then this will wrap
    // around back to zero and we can assume the first handle generated will have been dropped by
    // then.
    private val counter = java.util.concurrent.atomic.AtomicInteger(0)

    actual val size: kotlin.Int
        get() = map.size

    actual fun insert(obj: T): kotlin.ULong {
        val handle = counter.getAndAdd(1).toULong()
        map.put(handle, obj)
        return handle
    }

    actual fun get(handle: kotlin.ULong): T? {
        return map.get(handle)
    }

    actual fun remove(handle: kotlin.ULong): T? {
        return map.remove(handle)
    }
}
