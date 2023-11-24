{{- self.add_import("kotlinx.atomicfu.atomic") }}
{{- self.add_import("kotlinx.atomicfu.getAndUpdate") }}
{{- self.add_import("kotlinx.atomicfu.locks.reentrantLock") }}
{{- self.add_import("kotlinx.atomicfu.locks.withLock") }}

internal typealias Handle = kotlin.ULong
internal class ConcurrentHandleMap<T>(
    private val leftMap: MutableMap<Handle, T> = mutableMapOf(),
    private val rightMap: MutableMap<T, Handle> = mutableMapOf()
) {
    private val lock = reentrantLock()
    private val currentHandle = atomic(0L)

    fun insert(obj: T): Handle =
        lock.withLock {
            rightMap[obj] ?:
                currentHandle.getAndIncrement().toULong()
                    .also { handle ->
                        leftMap[handle] = obj
                        rightMap[obj] = handle
                    }
                }

    fun get(handle: Handle) = lock.withLock {
        leftMap[handle]
    }

    fun delete(handle: Handle) {
        this.remove(handle)
    }

    fun remove(handle: Handle): T? =
        lock.withLock {
            leftMap.remove(handle)?.let { obj ->
                rightMap.remove(obj)
                obj
            }
        }
}

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class ForeignCallback

// Magic number for the Rust proxy to call using the same mechanism as every other method,
// to free the callback once it's dropped by Rust.
internal const val IDX_CALLBACK_FREE = 0

// Callback return codes
private const val UNIFFI_CALLBACK_SUCCESS = 0
private const val UNIFFI_CALLBACK_ERROR = 1
private const val UNIFFI_CALLBACK_UNEXPECTED_ERROR = 2

internal abstract class FfiConverterCallbackInterface<CallbackInterface> : FfiConverter<CallbackInterface, Handle> {
    private val handleMap = ConcurrentHandleMap<CallbackInterface>()

    // Registers the foreign callback with the Rust side.
    // This method is generated for each callback interface.
    internal abstract fun register(lib: UniFFILib)

    fun drop(handle: Handle) {
        handleMap.remove(handle)
    }

    override fun lift(value: Handle): CallbackInterface {
        return handleMap.get(value) ?: throw InternalException("No callback in handlemap; this is a Uniffi bug")
    }

    override fun read(buf: NoCopySource) = lift(buf.readLong().toULong())

    override fun lower(value: CallbackInterface) =
        handleMap.insert(value).also {
            check(handleMap.get(it) === value) { "Handle map is not returning the object we just placed there. This is a bug in the HandleMap." }
        }

    override fun allocationSize(value: CallbackInterface) = 8

    override fun write(value: CallbackInterface, buf: Buffer) {
        buf.writeLong(lower(value).toLong())
    }
}
