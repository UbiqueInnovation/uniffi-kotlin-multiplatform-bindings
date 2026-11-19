package uniffi.runtime

abstract class FfiConverterCallbackInterface<CallbackInterface: Any>:
    FfiConverter<CallbackInterface, Long> {
    val handleMap = UniffiHandleMap<CallbackInterface>()

    fun drop(handle: Long) {
        handleMap.remove(handle)
    }

    override fun lift(value: Long): CallbackInterface {
        return handleMap.get(value)
    }

    override fun read(buf: ByteBuffer) = lift(buf.getLong())

    override fun lower(value: CallbackInterface) = handleMap.insert(value)

    override fun allocationSize(value: CallbackInterface) = 8UL

    override fun write(value: CallbackInterface, buf: ByteBuffer) {
        buf.putLong(lower(value))
    }
}
