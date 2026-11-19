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



// A borrowed view of foreign-owned bytes: the `ForeignBytes` a `&[u8]` argument
// (`[ByRef] bytes` in UDL) crosses the FFI as. Rust only ever reads through it,
// and only for the duration of the call - see `withForeignBytes`.

@Structure.FieldOrder("len", "data")
open class ForeignBytesStruct : Structure() {
    @JvmField var len: Int = 0
    @JvmField var data: Pointer? = null

    class ByValue : ForeignBytes(), Structure.ByValue
}

typealias ForeignBytes = ForeignBytesStruct

typealias ForeignBytesByValue = ForeignBytesStruct.ByValue

/**
 * Lend [value] to Rust as a `ForeignBytes` for the duration of [block].
 *
 * The bytes are copied into native memory that is freed as soon as [block] returns, so
 * the pointer Rust sees is only valid while the call is on the stack. That is exactly
 * the contract `ForeignBytes` documents on the Rust side, and it is why lowering a
 * borrowed byte slice has to be a scope rather than an expression.
 *
 * The copy is unavoidable here: a JVM `ByteArray` lives on the managed heap and has no
 * stable native address to hand out. Kotlin/Native pins the caller's array instead and
 * copies nothing.
 *
 * An empty array is passed as `(null, 0)`, which Rust lifts as an empty slice.
 */
inline fun <R> withForeignBytes(value: ByteArray, block: (ForeignBytesByValue) -> R): R {
    val foreignBytes = ForeignBytesByValue()
    if (value.isEmpty()) {
        foreignBytes.len = 0
        foreignBytes.data = null
        return block(foreignBytes)
    }
    return com.sun.jna.Memory(value.size.toLong()).use { memory ->
        memory.write(0L, value, 0, value.size)
        foreignBytes.len = value.size
        foreignBytes.data = memory
        block(foreignBytes)
    }
}


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
