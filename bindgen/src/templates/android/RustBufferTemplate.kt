// This is a helper for safely working with byte buffers returned from the Rust code.
// A rust-owned buffer is represented by its capacity, its current length, and a
// pointer to the underlying data.

@Structure.FieldOrder("capacity", "len", "data")
open class RustBufferStruct : Structure() {
    // Note: `capacity` and `len` are actually `ULong` values, but JVM only supports signed values.
    // When dealing with these fields, make sure to call `toULong()`.
    @JvmField internal var capacity: Long = 0
    @JvmField internal var len: Long = 0
    @JvmField internal var data: Pointer? = null

    class ByValue: RustBuffer(), Structure.ByValue
    class ByReference: RustBuffer(), Structure.ByReference
}

actual typealias RustBuffer = RustBufferStruct
internal actual var RustBuffer.capacity: Long
    get() = this.capacity
    set(value) { this.capacity = value }
internal actual var RustBuffer.len: Long
    get() = this.len
    set(value) { this.len = value }
internal actual var RustBuffer.data: Pointer?
    get() = this.data
    set(value) { this.data = value }
internal actual fun RustBuffer.asByteBuffer(): ByteBuffer? {
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
internal actual var RustBufferByValue.capacity: Long
    get() = this.capacity
    set(value) { this.capacity = value }
internal actual var RustBufferByValue.len: Long
    get() = this.len
    set(value) { this.len = value }
internal actual var RustBufferByValue.data: Pointer?
    get() = this.data
    set(value) { this.data = value }
internal actual fun RustBufferByValue.asByteBuffer(): ByteBuffer? {
    val ibuf = data?.getByteBuffer(0L, this.len.toLong()) ?: return null
    // Read java.nio.ByteBuffer into ByteArray
    val arr = ByteArray(ibuf.remaining())
    ibuf.get(arr)
    // Put ByteArray into common ByteBuffer
    val buffer = ByteBuffer()
    buffer.put(arr)
    return buffer
}


internal actual object RustBufferHelper
internal actual fun RustBufferHelper.allocFromByteBuffer(buffer: ByteBuffer): RustBufferByValue
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

internal actual class RustBufferByReference : com.sun.jna.ptr.ByReference(16)
internal actual fun RustBufferByReference.setValue(value: RustBufferByValue) {
    // NOTE: The offsets are as they are in the C-like struct.
    val pointer = getPointer()
    pointer.setLong(0, value.capacity)
    pointer.setLong(8, value.len)
    pointer.setPointer(16, value.data)
}
internal actual fun RustBufferByReference.getValue(): RustBufferByValue {
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
internal open class ForeignBytesStruct : Structure() {
    @JvmField internal var len: Int = 0
    @JvmField internal var data: Pointer? = null

    internal class ByValue : ForeignBytes(), Structure.ByValue
}

internal actual typealias ForeignBytes = ForeignBytesStruct
internal actual var ForeignBytes.len: Int
    get() = this.len
    set(value) { this.len = value }
internal actual var ForeignBytes.data: Pointer?
    get() = this.data
    set(value) { this.data = value }

internal actual typealias ForeignBytesByValue = ForeignBytesStruct.ByValue
internal actual var ForeignBytesByValue.len: Int
    get() = this.len
    set(value) { this.len = value }
internal actual var ForeignBytesByValue.data: Pointer?
    get() = this.data
    set(value) { this.data = value }
