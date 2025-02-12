@file:Suppress("CanBePrimaryConstructorProperty", "UnusedReceiverParameter")

package uniffi.runtime

import com.sun.jna.Callback
import com.sun.jna.Structure

actual fun foo() = "JVM"

//////// POINTER ////////
actual typealias Pointer = com.sun.jna.Pointer
actual val NullPointer: Pointer? = com.sun.jna.Pointer.NULL
actual fun getPointerNativeValue(ptr: Pointer): Long = Pointer.nativeValue(ptr)
actual fun Long.toPointer() = com.sun.jna.Pointer(this)

//////// HELPERS ////////
@Structure.FieldOrder("code", "errorBuf")
open class UniffiRustCallStatusStruct(
    code: Byte,
    errorBuf: RustBufferByValue,
) : Structure() {
    @JvmField var code: Byte = code
    @JvmField var errorBuf: RustBufferByValue = errorBuf

    constructor(): this(0.toByte(), RustBufferByValue())

    class ByValue(
        code: Byte,
        errorBuf: RustBufferByValue,
    ): UniffiRustCallStatusStruct(code, errorBuf), Structure.ByValue {
        constructor(): this(0.toByte(), RustBufferByValue())
    }
    class ByReference(
        code: Byte,
        errorBuf: RustBufferByValue,
    ): UniffiRustCallStatusStruct(code, errorBuf), Structure.ByReference {
        constructor(): this(0.toByte(), RustBufferByValue())
    }
}

typealias UniffiRustCallStatus = UniffiRustCallStatusStruct.ByReference
typealias UniffiRustCallStatusByValue = UniffiRustCallStatusStruct.ByValue

object UniffiRustCallStatusHelper
fun UniffiRustCallStatusHelper.allocValue(): UniffiRustCallStatusByValue
        = UniffiRustCallStatusByValue()
fun <U> UniffiRustCallStatusHelper.withReference(
    block: (UniffiRustCallStatus) -> U
): U {
    val status = UniffiRustCallStatus()
    return block(status)
}

/////// CALLBACKS ////////
// Define FFI callback types
interface UniffiRustFutureContinuationCallback: Callback {
    fun callback(handle: Long, pollResult: Byte)
}
interface UniffiForeignFutureFree: Callback {
    fun callback(handle: Long)
}
interface UniffiCallbackInterfaceFree: Callback {
    fun callback(handle: Long)
}
