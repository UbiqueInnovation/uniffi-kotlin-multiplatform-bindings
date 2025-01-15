
@Structure.FieldOrder("capacity", "len", "data")
open class RustBufferStruct : Structure() {
    // Note: `capacity` and `len` are actually `ULong` values, but JVM only supports signed values.
    // When dealing with these fields, make sure to call `toULong()`.
    @JvmField  var capacity: Long = 0
    @JvmField  var len: Long = 0
    @JvmField  var data: Pointer? = null

    class ByValue: RustBuffer(), Structure.ByValue
    class ByReference: RustBuffer(), Structure.ByReference
}

actual typealias RustBuffer = RustBufferStruct
 actual var RustBuffer.capacity: Long
    get() = this.capacity
    set(value) { this.capacity = value }
 actual var RustBuffer.len: Long
    get() = this.len
    set(value) { this.len = value }
 actual var RustBuffer.data: Pointer?
    get() = this.data
    set(value) { this.data = value }
 actual fun RustBuffer.asByteBuffer(): ByteBuffer? {
    val ibuf = data?.getByteBuffer(0L, this.len.toLong()) ?: return null
    // Read java.nio.ByteBuffer into ByteArray
    val arr = ByteArray(ibuf.remaining())
    ibuf.get(arr)
    // Put ByteArray into common ByteBuffer
    val buffer = ByteBuffer()
    buffer.put(arr)
    return buffer
}

actual typealias RustBufferByValue = RustBufferStruct.ByValue
 actual var RustBufferByValue.capacity: Long
    get() = this.capacity
    set(value) { this.capacity = value }
 actual var RustBufferByValue.len: Long
    get() = this.len
    set(value) { this.len = value }
 actual var RustBufferByValue.data: Pointer?
    get() = this.data
    set(value) { this.data = value }
 actual fun RustBufferByValue.asByteBuffer(): ByteBuffer? {
    val ibuf = data?.getByteBuffer(0L, this.len.toLong()) ?: return null
    // Read java.nio.ByteBuffer into ByteArray
    val arr = ByteArray(ibuf.remaining())
    ibuf.get(arr)
    // Put ByteArray into common ByteBuffer
    val buffer = ByteBuffer()
    buffer.put(arr)
    return buffer
}


 actual object RustBufferHelper
 actual fun RustBufferHelper.allocFromByteBuffer(buffer: ByteBuffer): RustBufferByValue
     = uniffiRustCall() { status ->
        // Note: need to convert the size to a `Long` value to make this work with JVM.
        UniffiLib.INSTANCE.{{ ci.ffi_rustbuffer_alloc().name() }}(buffer.internal().size.toLong(), status)!!
    }.also {
        val size = buffer.internal().size

        if(it.data == null) {
            throw RuntimeException("{{ ci.ffi_rustbuffer_alloc().name() }}() returned null data pointer (size=${size})")
        }

        var readPos = 0L
        it.writeField("len", size.toLong())
        // Loop until the buffer is completed read, okio reads max 8192 bytes
        while (readPos < size) {
            readPos += buffer.internal().read(it.data!!.getByteBuffer(readPos, size - readPos))
        }
    }

 actual class RustBufferByReference : com.sun.jna.ptr.ByReference(16)
 actual fun RustBufferByReference.setValue(value: RustBufferByValue) {
    // NOTE: The offsets are as they are in the C-like struct.
    val pointer = getPointer()
    pointer.setLong(0, value.capacity)
    pointer.setLong(8, value.len)
    pointer.setPointer(16, value.data)
}
 actual fun RustBufferByReference.getValue(): RustBufferByValue {
    val pointer = getPointer()
    val value = RustBufferByValue()
    value.writeField("capacity", pointer.getLong(0))
    value.writeField("len", pointer.getLong(8))
    value.writeField("data", pointer.getLong(16))
    return value
}



// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.

@Structure.FieldOrder("len", "data")
 open class ForeignBytesStruct : Structure() {
    @JvmField  var len: Int = 0
    @JvmField  var data: Pointer? = null

    class ByValue : ForeignBytes(), Structure.ByValue
}

 actual typealias ForeignBytes = ForeignBytesStruct
 actual var ForeignBytes.len: Int
    get() = this.len
    set(value) { this.len = value }
 actual var ForeignBytes.data: Pointer?
    get() = this.data
    set(value) { this.data = value }

 actual typealias ForeignBytesByValue = ForeignBytesStruct.ByValue
 actual var ForeignBytesByValue.len: Int
    get() = this.len
    set(value) { this.len = value }
 actual var ForeignBytesByValue.data: Pointer?
    get() = this.data
    set(value) { this.data = value }
