@file:OptIn(ExperimentalForeignApi::class)

package uniffi.runtime

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.cValue
import kotlinx.cinterop.pointed
import kotlinx.cinterop.readValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned

typealias RustBuffer = CPointer<cinterop.RustBuffer>

var RustBuffer.capacity: Long
    get() = pointed.capacity
    set(value) { pointed.capacity = value }
var RustBuffer.len: Long
    get() = pointed.len
    set(value) { pointed.len = value }
var RustBuffer.data: Pointer?
    get() = pointed.data?.let { Pointer(it) }
    set(value) { pointed.data = value?.inner?.reinterpret() }

fun RustBuffer.asByteBuffer(): ByteBuffer? {
    require(pointed.len <= Int.MAX_VALUE) {
        val length = pointed.len
        "cannot handle RustBuffer longer than Int.MAX_VALUE bytes: length is $length"
    }
    return ByteBuffer(
        pointed.data?.reinterpret<kotlinx.cinterop.ByteVar>() ?: return null,
        pointed.len.toInt(),
    )
}

typealias RustBufferByValue = CValue<cinterop.RustBuffer>
fun RustBufferByValue(
    capacity: Long,
    len: Long,
    data: Pointer?,
): RustBufferByValue {
    return cValue<cinterop.RustBuffer> {
        this.capacity = capacity
        this.len = len
        this.data = data?.inner?.reinterpret()
    }
}
val RustBufferByValue.capacity: Long
    get() = useContents { capacity }
val RustBufferByValue.len: Long
    get() = useContents { len }
val RustBufferByValue.data: Pointer?
    get() = useContents { data }?.let { Pointer(it) }
fun RustBufferByValue.asByteBuffer(): ByteBuffer? {
    require(len <= Int.MAX_VALUE) {
        val length = len
        "cannot handle RustBuffer longer than Int.MAX_VALUE bytes: length is $length"
    }
    return ByteBuffer(
        data?.inner?.reinterpret() ?: return null,
        len.toInt(),
    )
}

/**
 * The equivalent of the `*mut RustBuffer` type.
 * Required for callbacks taking in an out pointer.
 *
 * Size is the sum of all values in the struct.
 */
typealias RustBufferByReference = CPointer<cinterop.RustBufferByReference>

fun RustBufferByReference.setValue(value: RustBufferByValue) {
    pointed.capacity = value.capacity
    pointed.len = value.len
    pointed.data = value.data?.inner?.reinterpret()
}
fun RustBufferByReference.getValue(): RustBufferByValue
        = pointed.reinterpret<cinterop.RustBuffer>().readValue()


typealias ForeignBytes = CPointer<cinterop.ForeignBytes>
var ForeignBytes.len: Int
    get() = pointed.len
    set(value) { pointed.len = value }
var ForeignBytes.data: Pointer?
    get() = pointed.data?.let { Pointer(it) }
    set(value) { pointed.data = value?.inner?.reinterpret() }

typealias ForeignBytesByValue = CValue<cinterop.ForeignBytes>
val ForeignBytesByValue.len: Int
    get() = useContents { len }
val ForeignBytesByValue.data: Pointer?
    get() = useContents { data }?.let { Pointer(it) }

/**
 * Lend [value] to Rust as a `ForeignBytes` for the duration of [block].
 *
 * The array is pinned rather than copied, so Rust reads the caller's bytes in place.
 * The pin is released as soon as [block] returns, which makes the pointer valid only
 * while the call is on the stack - exactly the contract `ForeignBytes` documents on
 * the Rust side, and why lowering a borrowed byte slice has to be a scope rather than
 * an expression.
 *
 * An empty array is passed as `(null, 0)`, which Rust lifts as an empty slice. It also
 * cannot be pinned: there is no element 0 to take the address of.
 */
inline fun <R> withForeignBytes(value: ByteArray, block: (ForeignBytesByValue) -> R): R {
    if (value.isEmpty()) {
        return block(cValue { len = 0; data = null })
    }
    return value.usePinned { pinned ->
        block(cValue { len = value.size; data = pinned.addressOf(0).reinterpret() })
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

    fun free(buf: RustBufferByValue): Unit = uniffiRustCall() { status ->
        UniffiLib.INSTANCE.ffi_uniffi_runtime_rustbuffer_free(buf, status)
    }
}