import java.util.concurrent.ConcurrentHashMap

actual class UniFfiHandleMap<T: Any> {
    private val map = ConcurrentHashMap<ULong, T>()
    // Use AtomicInteger for our counter, since we may be on a 32-bit system.  4 billion possible
    // values seems like enough. If somehow we generate 4 billion handles, then this will wrap
    // around back to zero and we can assume the first handle generated will have been dropped by
    // then.
    private val counter = java.util.concurrent.atomic.AtomicInteger(0)

    actual val size: Int
        get() = map.size

    actual fun insert(obj: T): ULong {
        val handle = counter.getAndAdd(1).toULong()
        map.put(handle, obj)
        return handle
    }

    actual fun get(handle: ULong): T? {
        return map.get(handle)
    }

    actual fun remove(handle: ULong) {
        map.remove(handle)
    }
}
