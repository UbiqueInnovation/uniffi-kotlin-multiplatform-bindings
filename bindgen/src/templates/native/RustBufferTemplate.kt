
internal actual typealias RustBuffer = CPointer<{{ ci.namespace() }}.cinterop.RustBuffer>

internal actual var RustBuffer.capacity: Long
    get() = pointed.capacity
    set(value) { pointed.capacity = value }
internal actual var RustBuffer.len: Long
    get() = pointed.len
    set(value) { pointed.len = value }
internal actual var RustBuffer.data: Pointer?
    get() = pointed.data
    set(value) { pointed.data = value?.reinterpret() }
internal actual fun RustBuffer.asByteBuffer(): ByteBuffer? {
    val buffer = ByteBuffer()
    val data = pointed.data?.reinterpret<kotlinx.cinterop.ByteVar>() ?: return null

    if (pointed.len < 0L)
        throw IllegalStateException("Trying to call asByteBuffer with negative length")

    if (pointed.len == 0L)
        return buffer

    // Copy over bytes 1 by 1
    for (i in 0..len - 1) {
        buffer.put(data[i])
    }
    
    return buffer
}

internal actual typealias RustBufferByValue = CValue<{{ ci.namespace() }}.cinterop.RustBuffer>
internal actual var RustBufferByValue.capacity: Long
    get() = useContents { capacity }
    set(value) { println("tried writing value") }
internal actual var RustBufferByValue.len: Long
    get() = useContents { len }
    set(value) {println("tried writing value") }
internal actual var RustBufferByValue.data: Pointer?
    get() = useContents { data }
    set(value) { println("tried writing value") }
internal actual fun RustBufferByValue.asByteBuffer(): ByteBuffer? {
    val buffer = ByteBuffer()
    val data = useContents { data }?.reinterpret<kotlinx.cinterop.ByteVar>() ?: return null
    val len = useContents { len }
    if (len < 0L)
        throw IllegalStateException("Trying to call asByteBuffer with negative length")

    if (len == 0L)
        return buffer

    // Copy over bytes 1 by 1
    for (i in 0..<len) {
        buffer.put(data[i])
    }
    
    return buffer   
}

internal actual object RustBufferHelper
internal actual fun RustBufferHelper.allocFromByteBuffer(buffer: ByteBuffer): RustBufferByValue
     = uniffiRustCall() { status ->
        // Note: need to convert the size to a `Long` value to make this work with JVM.
        UniffiLib.INSTANCE.{{ ci.ffi_rustbuffer_alloc().name() }}(buffer.internal().size.toLong(), status)!!
    }.also {
        val size = buffer.internal().size
        it.useContents {
            val notNullData = data
            checkNotNull(notNullData) { "RustBuffer.alloc() returned null data pointer (size=${size})" }

           for (i in 0..<size) {
                notNullData[i.toInt()] = buffer.get().toUByte()
           }

        }
    }

internal actual typealias RustBufferByReference = CPointer<{{ ci.namespace() }}.cinterop.RustBufferByReference>

// TODO: Implement reading/writing to pointer value inside CPointer
internal actual fun RustBufferByReference.setValue(value: RustBufferByValue) {
    TODO("Not implemented yet!")
}
internal actual fun RustBufferByReference.getValue(): RustBufferByValue
    = TODO("Not implemented yet!")


internal actual typealias ForeignBytes = CPointer<{{ ci.namespace() }}.cinterop.ForeignBytes>
internal actual var ForeignBytes.len: Int
    get() = pointed.len
    set(value) { pointed.len = value }
internal actual var ForeignBytes.data: Pointer?
    get() = pointed.data
    set(value) { pointed.data = value?.reinterpret() }

internal actual typealias ForeignBytesByValue = CValue<{{ ci.namespace() }}.cinterop.ForeignBytes>
internal actual var ForeignBytesByValue.len: Int
    get() = useContents { len }
    set(value) { TODO("Not implemented yet!") }
internal actual var ForeignBytesByValue.data: Pointer?
    get() = useContents { data }
    set(value) { TODO("Not implemented yet!") }
