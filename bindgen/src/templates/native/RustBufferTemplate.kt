
public actual class RustBuffer(val inner: CPointer<{{ ci.namespace() }}.cinterop.RustBuffer>) {}

public actual var RustBuffer.capacity
    get() = this.inner.pointed.capacity
    set(value) { this.inner.pointed.capacity = value }

public actual var RustBuffer.len: Long
    get() = this.inner.pointed.len
    set(value) { this.inner.pointed.len = value }

public actual var RustBuffer.data: Pointer?
    get() = this.inner.pointed.data?.let { Pointer(it) }
    set(value) { this.inner.pointed.data = value?.inner?.reinterpret() }

public actual fun RustBuffer.asByteBuffer(): ByteBuffer? {
    val buffer = ByteBuffer()
    val data = this.inner.pointed.data?.reinterpret<kotlinx.cinterop.ByteVar>() ?: return null

    if (this.inner.pointed.len < 0L)
        throw IllegalStateException("Trying to call asByteBuffer with negative length")

    if (this.inner.pointed.len == 0L)
        return buffer

    // Copy over bytes 1 by 1
    for (i in 0..len - 1) {
        buffer.put(data[i])
    }
    
    return buffer
}

public actual class RustBufferByValue(val inner: CValue<{{ ci.namespace() }}.cinterop.RustBuffer>) {}

public actual var RustBufferByValue.capacity: Long
    get() = inner.useContents { capacity }
    set(value) { println("tried writing value") }

public actual var RustBufferByValue.len: Long
    get() = inner.useContents { len }
    set(value) {println("tried writing value") }

public actual var RustBufferByValue.data: Pointer?
    get() = inner.useContents { data?.let { Pointer(it) } }
    set(value) { println("tried writing value") }

public actual fun RustBufferByValue.asByteBuffer(): ByteBuffer? {
    val buffer = ByteBuffer()
    val data = inner.useContents { data }?.reinterpret<kotlinx.cinterop.ByteVar>() ?: return null
    val len = inner.useContents { len }
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

public actual object RustBufferHelper
public actual fun RustBufferHelper.allocFromByteBuffer(buffer: ByteBuffer): RustBufferByValue
     = uniffiRustCall() { status ->
        // Note: need to convert the size to a `Long` value to make this work with JVM.
        UniffiLib.INSTANCE.{{ ci.ffi_rustbuffer_alloc().name() }}(buffer.internal().size.toLong(), status)!!
    }.also {
        val size = buffer.internal().size
        it.inner.useContents {
            val notNullData = data
            checkNotNull(notNullData) { "RustBuffer.alloc() returned null data pointer (size=${size})" }

           for (i in 0..<size) {
                notNullData[i.toInt()] = buffer.get().toUByte()
           }
        }
    }

public actual class RustBufferByReference(val inner: CPointer<{{ ci.namespace() }}.cinterop.RustBufferByReference>) {}

public actual fun RustBufferByReference.setValue(value: RustBufferByValue) {
    inner.pointed.capacity = value.capacity
    inner.pointed.len = value.len
    inner.pointed.data = value.data?.inner?.reinterpret()
}
public actual fun RustBufferByReference.getValue(): RustBufferByValue
    = RustBufferByValue(inner.pointed.reinterpret<{{ ci.namespace() }}.cinterop.RustBuffer>().readValue())


public actual class ForeignBytes(val inner: CPointer<{{ ci.namespace() }}.cinterop.ForeignBytes>) {}

public actual var ForeignBytes.len: Int
    get() = inner.pointed.len
    set(value) { inner.pointed.len = value }
public actual var ForeignBytes.data: Pointer?
    get() = inner.pointed.data?.let { Pointer(it) }
    set(value) { inner.pointed.data = value?.inner?.reinterpret() }

public actual class ForeignBytesByValue(val inner: CValue<{{ ci.namespace() }}.cinterop.ForeignBytes>) {}

public actual var ForeignBytesByValue.len: Int
    get() = inner.useContents { len }
    set(value) { TODO("Not implemented yet!") }
public actual var ForeignBytesByValue.data: Pointer?
    get() = inner.useContents { data?.let { Pointer(it) } }
    set(value) { TODO("Not implemented yet!") }
