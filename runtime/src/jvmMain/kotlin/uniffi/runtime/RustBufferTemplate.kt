@file:Suppress("CanBePrimaryConstructorProperty")

package uniffi.runtime

import com.sun.jna.Structure

@Structure.FieldOrder("capacity", "len", "data")
open class RustBufferStruct(
    capacity: Long,
    len: Long,
    data: Pointer?,
) : Structure() {
    // Note: `capacity` and `len` are actually `ULong` values, but JVM only supports signed values.
    // When dealing with these fields, make sure to call `toULong()`.
    @JvmField var capacity: Long = capacity
    @JvmField var len: Long = len
    @JvmField var data: Pointer? = data

    constructor(): this(0.toLong(), 0.toLong(), null)

    class ByValue(
        capacity: Long,
        len: Long,
        data: Pointer?,
    ): RustBuffer(capacity, len, data), Structure.ByValue {
        constructor(): this(0.toLong(), 0.toLong(), null)
    }

    /**
     * The equivalent of the `*mut RustBuffer` type.
     * Required for callbacks taking in an out pointer.
     *
     * Size is the sum of all values in the struct.
     */
    class ByReference(
        capacity: Long,
        len: Long,
        data: Pointer?,
    ): RustBuffer(capacity, len, data), Structure.ByReference {
        constructor(): this(0.toLong(), 0.toLong(), null)
    }
}

typealias RustBuffer = RustBufferStruct
fun RustBuffer.asByteBuffer(): ByteBuffer? {
    require(this.len <= Int.MAX_VALUE) {
        "cannot handle RustBuffer longer than Int.MAX_VALUE bytes: length is ${this.len}"
    }
    return ByteBuffer(data?.getByteBuffer(0L, this.len) ?: return null)
}

typealias RustBufferByValue = RustBufferStruct.ByValue
fun RustBufferByValue.asByteBuffer(): ByteBuffer? {
    require(this.len <= Int.MAX_VALUE) {
        "cannot handle RustBuffer longer than Int.MAX_VALUE bytes: length is ${this.len}"
    }
    return ByteBuffer(data?.getByteBuffer(0L, this.len) ?: return null)
}

class RustBufferByReference : com.sun.jna.ptr.ByReference(16)
fun RustBufferByReference.setValue(value: RustBufferByValue) {
    // NOTE: The offsets are as they are in the C-like struct.
    pointer.setLong(0, value.capacity)
    pointer.setLong(8, value.len)
    pointer.setPointer(16, value.data)
}
fun RustBufferByReference.getValue(): RustBufferByValue {
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
    @JvmField var len: Int = 0
    @JvmField var data: Pointer? = null

    class ByValue : ForeignBytes(), Structure.ByValue
}

typealias ForeignBytes = ForeignBytesStruct

typealias ForeignBytesByValue = ForeignBytesStruct.ByValue


fun RustBuffer.setValue(array: RustBufferByValue) {
    this.data = array.data
    this.len = array.len
    this.capacity = array.capacity
}

object RustBufferHelper {
    fun allocValue(size: ULong = 0UL): RustBufferByValue = uniffiRustCall() { status ->
        // Note: need to convert the size to a `Long` value to make this work with JVM.
        UniffiLib.INSTANCE.ffi_uniffi_runtime_rustbuffer_alloc(size.toLong(), status)
    }.also {
        if(it.data == null) {
            throw RuntimeException("RustBuffer.alloc() returned null data pointer (size=${size})")
        }
    }

    fun free(buf: RustBufferByValue) = uniffiRustCall() { status ->
        UniffiLib.INSTANCE.ffi_uniffi_runtime_rustbuffer_free(buf, status)
    }
}
