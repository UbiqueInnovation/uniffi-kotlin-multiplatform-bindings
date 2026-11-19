package uniffi.runtime

import kotlinx.atomicfu.AtomicLong
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.ReentrantLock

class UniffiHandleMap<T: Any> {
    private companion object {
        // Both Rust and foreign handles travel across the FFI for trait interfaces, and the two are told
        // apart by the lowest bit: it is always set for foreign handles and never for Rust ones. Starting
        // at 1 and stepping by 2 keeps every handle we hand out odd.
        // (Starting at 1 also avoids "Null Pointers" in native's `interpretCPointer`.)
        private const val UNIFFI_HANDLEMAP_INITIAL = 1L
        private const val UNIFFI_HANDLEMAP_DELTA = 2L
    }

    // NOTE: java.util.concurrent.ConcurrentMap will be more efficient than a coarse grained lock
    //       use it on JVM platforms
    private val mapLock = ReentrantLock()
    private val map = HashMap<Long, T>()

    private val counter: AtomicLong = atomic(UNIFFI_HANDLEMAP_INITIAL)

    val size: Int
        get() = map.size

    // Insert a new object into the handle map and get a handle for it
    fun insert(obj: T): Long {
        val handle = counter.getAndAdd(UNIFFI_HANDLEMAP_DELTA)
        syncAccess { map.put(handle, obj) }
        return handle
    }

    fun clone(handle: Long): Long {
        val obj = syncAccess { map[handle] }
            ?: throw InternalException("UniffiHandleMap.clone: Invalid handle")
        return insert(obj)
    }

    // Get an object from the handle map
    fun get(handle: Long): T {
        return syncAccess { map[handle] } ?: throw InternalException("UniffiHandleMap.get: Invalid handle")
    }

    // Remove an entry from the handle-map and get the Kotlin object back
    fun remove(handle: Long): T {
        return syncAccess { map.remove(handle) } ?: throw InternalException("UniffiHandleMap: Invalid handle")
    }

    fun <T> syncAccess(block: () -> T): T {
        mapLock.lock()
        try {
            return block()
        } finally {
            mapLock.unlock()
        }
    }
}
