import kotlinx.coroutines.CoroutineScope
import okio.Buffer

object FfiConverterForeignExecutor: FfiConverter<CoroutineScope, ULong> {
    internal val handleMap = UniFfiHandleMap<CoroutineScope>()
    internal val foreignExecutorCallback = createUniFfiForeignExecutorCallback()

    internal fun drop(handle: ULong) {
        handleMap.remove(handle)
    }

    internal fun register(lib: UniFFILib) {
        lib.uniffi_foreign_executor_callback_set(foreignExecutorCallback)
    }

    // Number of live handles, exposed so we can test the memory management
    public fun handleCount() : Int {
        return handleMap.size
    }

    override fun allocationSize(value: CoroutineScope) = ULong.SIZE_BYTES

    override fun lift(value: ULong): CoroutineScope {
        return handleMap.get(value) ?: throw RuntimeException("unknown handle in FfiConverterForeignExecutor.lift")
    }

    override fun read(source: NoCopySource): CoroutineScope = TODO("unused")

    override fun lower(value: CoroutineScope): ULong {
        return handleMap.insert(value)
    }

    override fun write(value: CoroutineScope, buf: Buffer) = TODO("unused")

}
