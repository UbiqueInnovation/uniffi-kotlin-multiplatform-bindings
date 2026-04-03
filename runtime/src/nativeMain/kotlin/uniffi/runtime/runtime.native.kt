@file:OptIn(ExperimentalForeignApi::class)

package uniffi.runtime

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.useContents
import kotlinx.cinterop.alloc
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.write

actual fun foo() = "Native"

//////// POINTER ////////
typealias GenericPointer = CPointer<out CPointed>
actual class Pointer (val inner: GenericPointer) {
    actual constructor(value: Long) : this(requireNotNull(value.toCPointer()))
}
actual val NullPointer: Pointer? = null
actual fun getPointerNativeValue(ptr: Pointer): Long = ptr.inner.rawValue.toLong()
actual fun kotlin.Long.toPointer(): Pointer = Pointer(requireNotNull(this.toCPointer()))

//////// HELPERS ////////
typealias UniffiRustCallStatus = CPointer<cinterop.UniffiRustCallStatus>
var UniffiRustCallStatus.code: Byte
    get() = pointed.code
    set(value) { pointed.code = value }
var UniffiRustCallStatus.errorBuf: RustBufferByValue
    get() = pointed.errorBuf.readValue()
    set(value) { value.place(pointed.errorBuf.ptr) }

typealias UniffiRustCallStatusByValue = CValue<cinterop.UniffiRustCallStatus>
fun UniffiRustCallStatusByValue(
    code: Byte,
    errorBuf: RustBufferByValue
): UniffiRustCallStatusByValue {
    return cValue<cinterop.UniffiRustCallStatus> {
        this.code = code
        errorBuf.write(this.errorBuf.rawPtr)
    }
}
val UniffiRustCallStatusByValue.code: Byte
    get() = useContents { code }
val UniffiRustCallStatusByValue.errorBuf: RustBufferByValue
    get() = useContents { errorBuf.readValue() }

object UniffiRustCallStatusHelper
fun UniffiRustCallStatusHelper.allocValue(): UniffiRustCallStatusByValue
        = cValue<cinterop.UniffiRustCallStatus>()
fun <U> UniffiRustCallStatusHelper.withReference(
    block: (UniffiRustCallStatus) -> U
): U {
    return memScoped {
        val status = alloc<cinterop.UniffiRustCallStatus>()
        block(status.ptr)
    }
}

/////// CALLBACKS ////////
// Define FFI callback types
typealias UniffiRustFutureContinuationCallback = cinterop.UniffiRustFutureContinuationCallback
typealias UniffiForeignFutureFree = cinterop.UniffiForeignFutureFree
typealias UniffiCallbackInterfaceFree = cinterop.UniffiCallbackInterfaceFree

