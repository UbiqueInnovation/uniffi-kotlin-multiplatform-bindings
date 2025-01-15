
expect class RustBuffer
 expect var RustBuffer.capacity: Long
 expect var RustBuffer.len: Long
 expect var RustBuffer.data: Pointer?
 expect fun RustBuffer.asByteBuffer(): ByteBuffer?

fun RustBuffer.setValue(array: RustBufferByValue) {
    this.data = array.data
    this.len = array.len
    this.capacity = array.capacity
}

expect class RustBufferByValue
 expect var RustBufferByValue.capacity: Long
 expect var RustBufferByValue.len: Long
 expect var RustBufferByValue.data: Pointer?
 expect fun RustBufferByValue.asByteBuffer(): ByteBuffer?

 expect object RustBufferHelper
 expect fun RustBufferHelper.allocFromByteBuffer(buffer: ByteBuffer): RustBufferByValue
 fun RustBufferHelper.allocValue(size: ULong = 0UL): RustBufferByValue = uniffiRustCall() { status ->
    // Note: need to convert the size to a `Long` value to make this work with JVM.
    UniffiLib.INSTANCE.{{ ci.ffi_rustbuffer_alloc().name() }}(size.toLong(), status)
}.also {
    if(it.data == null) {
        throw RuntimeException("RustBuffer.alloc() returned null data pointer (size=${size})")
    }
}
 fun RustBufferHelper.free(buf: RustBufferByValue) = uniffiRustCall() { status ->
    UniffiLib.INSTANCE.{{ ci.ffi_rustbuffer_free().name() }}(buf, status)!!
}

/**
 * The equivalent of the `*mut RustBuffer` type.
 * Required for callbacks taking in an out pointer.
 *
 * Size is the sum of all values in the struct.
 */
 expect class RustBufferByReference
/**
 * Set the pointed-to `RustBuffer` to the given value.
 */
 expect fun RustBufferByReference.setValue(value: RustBufferByValue)
/**
 * Get a `RustBufferByValue` from this reference.
 */ 
 expect fun RustBufferByReference.getValue(): RustBufferByValue


// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.
 expect class ForeignBytes
 expect var ForeignBytes.len: Int
 expect var ForeignBytes.data: Pointer?

 expect class ForeignBytesByValue
 expect var ForeignBytesByValue.len: Int
 expect var ForeignBytesByValue.data: Pointer?
