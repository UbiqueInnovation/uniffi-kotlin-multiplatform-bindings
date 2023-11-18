{{ self.add_import("kotlinx.coroutines.CoroutineScope") }}
{{ self.add_import("kotlinx.coroutines.delay") }}
{{ self.add_import("kotlinx.coroutines.isActive") }}
{{ self.add_import("kotlinx.coroutines.launch") }}

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class UniFfiForeignExecutorCallback

expect fun createUniFfiForeignExecutorCallback(): UniFfiForeignExecutorCallback

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
