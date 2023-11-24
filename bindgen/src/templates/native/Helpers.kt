// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustCallStatus = CPointer<{{ ci.namespace() }}.cinterop.RustCallStatus>

actual val RustCallStatus.statusCode: kotlin.Byte
    get() = pointed.code
actual val RustCallStatus.errorBuffer: RustBuffer
    get() = pointed.errorBuf.readValue()

actual fun <T> withRustCallStatus(block: (RustCallStatus) -> T): T =
    memScoped {
        val allocated = alloc<{{ ci.namespace() }}.cinterop.RustCallStatus>().ptr
        block(allocated)
    }

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustCallStatusByValue = kotlinx.cinterop.CValue<{{ ci.namespace() }}.cinterop.RustCallStatus>

val RustCallStatusByValue.statusCode: kotlin.Byte
    get() = useContents { code }

// This is actually common kotlin but inefficient because of the coarse granular locking...
// TODO either create some real implementation or at least measure if protecting the counter
//      with the lock and using a plain Int wouldn't be faster
internal actual class UniFfiHandleMap<T : Any> {
    private val mapLock = kotlinx.atomicfu.locks.ReentrantLock()
    private val map = HashMap<kotlin.ULong, T>()

    // Use AtomicInteger for our counter, since we may be on a 32-bit system.  4 billion possible
    // values seems like enough. If somehow we generate 4 billion handles, then this will wrap
    // around back to zero and we can assume the first handle generated will have been dropped by
    // then.
    private val counter: kotlinx.atomicfu.AtomicInt = kotlinx.atomicfu.atomic(0)

    actual val size: kotlin.Int
        get() = map.size

    actual fun insert(obj: T): kotlin.ULong {
        val handle = counter.getAndIncrement().toULong()
        synchronizedMapAccess { map.put(handle, obj) }
        return handle
    }

    actual fun get(handle: kotlin.ULong): T? {
        return synchronizedMapAccess { map.get(handle) }
    }

    actual fun remove(handle: kotlin.ULong): T? {
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
