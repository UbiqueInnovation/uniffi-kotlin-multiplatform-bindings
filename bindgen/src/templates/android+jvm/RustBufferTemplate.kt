{% include "ffi/RustBufferTemplate.kt" %}

@Structure.FieldOrder("capacity", "len", "data")
open class RustBufferStruct(
    capacity: Long,
    len: Long,
    data: Pointer?,
) : Structure() {
    // Note: `capacity` and `len` are actually `ULong` values, but JVM only supports signed values.
    // When dealing with these fields, make sure to call `toULong()`.
    @JvmField internal var capacity: Long = capacity
    @JvmField internal var len: Long = len
    @JvmField internal var data: Pointer? = data

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
internal var RustBuffer.capacity: Long
    get() = this.capacity
    set(value) { this.capacity = value }
internal var RustBuffer.len: Long
    get() = this.len
    set(value) { this.len = value }
internal var RustBuffer.data: Pointer?
    get() = this.data
    set(value) { this.data = value }
internal fun RustBuffer.asByteBuffer(): ByteBuffer? {
    {% call kt::check_rust_buffer_length("this.len") %}
    return ByteBuffer(data?.getByteBuffer(0L, this.len) ?: return null)
}

typealias RustBufferByValue = RustBufferStruct.ByValue
internal val RustBufferByValue.capacity: Long
    get() = this.capacity
internal val RustBufferByValue.len: Long
    get() = this.len
internal val RustBufferByValue.data: Pointer?
    get() = this.data
internal fun RustBufferByValue.asByteBuffer(): ByteBuffer? {
    {% call kt::check_rust_buffer_length("this.len") %}
    return ByteBuffer(data?.getByteBuffer(0L, this.len) ?: return null)
}

internal class RustBufferByReference : com.sun.jna.ptr.ByReference(16)
internal fun RustBufferByReference.setValue(value: RustBufferByValue) {
    // NOTE: The offsets are as they are in the C-like struct.
    val pointer = getPointer()
    pointer.setLong(0, value.capacity)
    pointer.setLong(8, value.len)
    pointer.setPointer(16, value.data)
}
internal fun RustBufferByReference.getValue(): RustBufferByValue {
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

internal typealias ForeignBytes = ForeignBytesStruct
internal var ForeignBytes.len: Int
    get() = this.len
    set(value) { this.len = value }
internal var ForeignBytes.data: Pointer?
    get() = this.data
    set(value) { this.data = value }

internal typealias ForeignBytesByValue = ForeignBytesStruct.ByValue
internal val ForeignBytesByValue.len: Int
    get() = this.len
internal val ForeignBytesByValue.data: Pointer?
    get() = this.data
