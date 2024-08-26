// This is a helper for safely working with byte buffers returned from the Rust code.
// A rust-owned buffer is represented by its capacity, its current length, and a
// pointer to the underlying data.

expect class RustBuffer
internal expect var RustBuffer.capacity: Long
internal expect var RustBuffer.len: Long
internal expect var RustBuffer.data: Pointer?
internal expect fun RustBuffer.asByteBuffer(): ByteBuffer?

fun RustBuffer.setValue(array: RustBufferByValue) {
    this.data = array.data
    this.len = array.len
    this.capacity = array.capacity
}

expect class RustBufferByValue
internal expect var RustBufferByValue.capacity: Long
internal expect var RustBufferByValue.len: Long
internal expect var RustBufferByValue.data: Pointer?
internal expect fun RustBufferByValue.asByteBuffer(): ByteBuffer?

internal expect object RustBufferHelper
internal expect fun RustBufferHelper.allocFromByteBuffer(buffer: ByteBuffer): RustBufferByValue
internal fun RustBufferHelper.allocValue(size: ULong = 0UL): RustBufferByValue = uniffiRustCall() { status ->
    // Note: need to convert the size to a `Long` value to make this work with JVM.
    UniffiLib.INSTANCE.{{ ci.ffi_rustbuffer_alloc().name() }}(size.toLong(), status)
}.also {
    if(it.data == null) {
        throw RuntimeException("RustBuffer.alloc() returned null data pointer (size=${size})")
    }
}
internal fun RustBufferHelper.free(buf: RustBufferByValue) = uniffiRustCall() { status ->
    UniffiLib.INSTANCE.{{ ci.ffi_rustbuffer_free().name() }}(buf, status)!!
}

/**
 * The equivalent of the `*mut RustBuffer` type.
 * Required for callbacks taking in an out pointer.
 *
 * Size is the sum of all values in the struct.
 */
internal expect class RustBufferByReference
/**
 * Set the pointed-to `RustBuffer` to the given value.
 */
internal expect fun RustBufferByReference.setValue(value: RustBufferByValue)
/**
 * Get a `RustBufferByValue` from this reference.
 */ 
internal expect fun RustBufferByReference.getValue(): RustBufferByValue


// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.
internal expect class ForeignBytes
internal expect var ForeignBytes.len: Int
internal expect var ForeignBytes.data: Pointer?

internal expect class ForeignBytesByValue
internal expect var ForeignBytesByValue.len: Int
internal expect var ForeignBytesByValue.data: Pointer?
