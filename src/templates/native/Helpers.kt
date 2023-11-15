// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustCallStatus = CPointer<{{ config.package_name() }}.cinterop.RustCallStatus>

actual val RustCallStatus.statusCode: Byte
    get() = pointed.code
actual val RustCallStatus.errorBuffer: RustBuffer
    get() = pointed.errorBuf.readValue()

actual fun <T> withRustCallStatus(block: (RustCallStatus) -> T): T =
    memScoped {
        val allocated = alloc<{{ config.package_name() }}.cinterop.RustCallStatus>().ptr
        block(allocated)
    }

val RustCallStatusByValue.statusCode: Byte
    get() = useContents { code }

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustCallStatusByValue = CValue<{{ config.package_name() }}.cinterop.RustCallStatus>

// This is actually common kotlin but inefficient because of the coarse granular locking...
// TODO either create some real implementation or at least measure if protecting the counter
//      with the lock and using a plain Int wouldn't be faster
actual class UniFfiHandleMap<T : Any> {
    private val mapLock = kotlinx.atomicfu.locks.ReentrantLock()
    private val map = HashMap<ULong, T>()

    // Use AtomicInteger for our counter, since we may be on a 32-bit system.  4 billion possible
    // values seems like enough. If somehow we generate 4 billion handles, then this will wrap
    // around back to zero and we can assume the first handle generated will have been dropped by
    // then.
    private val counter = kotlinx.atomicfu.atomic<Int>(0)

    actual val size: Int
        get() = map.size

    actual fun insert(obj: T): ULong {
        val handle = counter.getAndUpdate { it + 1 }.toULong()
        synchronizedMapAccess { map.put(handle, obj) }
        return handle
    }

    actual fun get(handle: ULong): T? {
        return synchronizedMapAccess { map.get(handle) }
    }

    actual fun remove(handle: ULong): T? {
        return synchronizedMapAccess { map.remove(handle) }
    }

    fun <T> synchronizedMapAccess(block: () -> T): T {
        mapLock.lock()
        try {
            return block()
        } finally {
            mapLock.unlock()
        }
    }
}

// FFI type for Rust future continuations

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
internal actual typealias UniFfiRustFutureContinuationCallbackType = CPointer<CFunction<(ULong, Short) -> Unit>>

internal actual fun createUniFfiRustFutureContinuationCallback(): UniFfiRustFutureContinuationCallbackType =
    staticCFunction<ULong, Short, Unit> { continuationHandle: ULong, pollResult: Short ->
        resumeContinutation(continuationHandle, pollResult)
    }
