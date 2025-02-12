@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "CanBePrimaryConstructorProperty")

package uniffi.runtime

import com.sun.jna.Callback
import com.sun.jna.Structure

actual fun foo() = "JVM"

//////// RUNTIME ////////
interface RuntimeInt {
    fun allocRustBuffer(
        size: ULong,
    ): RustBufferByValue

    fun freeRustBuffer(
        buf: RustBufferByValue
    ): Unit
}

object DummyRuntime : RuntimeInt {
    override fun allocRustBuffer(
        size: ULong,
    ): RustBufferByValue = TODO()

    override fun freeRustBuffer(
        buf: RustBufferByValue,
    ) = TODO()
}

// NOTE: Don't think this will work with multiple stuff, how to do it?
//       Maybe should have it's own library that can handle this stuff
//       but is the memory shared then?
var RUNTIME: RuntimeInt = DummyRuntime

//////// POINTER ////////
actual typealias Pointer = com.sun.jna.Pointer
actual val NullPointer: Pointer? = com.sun.jna.Pointer.NULL
actual fun getPointerNativeValue(ptr: Pointer): Long = Pointer.nativeValue(ptr)
actual fun kotlin.Long.toPointer() = com.sun.jna.Pointer(this)

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
var UniffiRustCallStatus.code: Byte
    get() = this.code
    set(value) { this.code = value }
var UniffiRustCallStatus.errorBuf: RustBufferByValue
    get() = this.errorBuf
    set(value) { this.errorBuf = value }

typealias UniffiRustCallStatusByValue = UniffiRustCallStatusStruct.ByValue
val UniffiRustCallStatusByValue.code: Byte
    get() = this.code
val UniffiRustCallStatusByValue.errorBuf: RustBufferByValue
    get() = this.errorBuf

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
    fun callback(handle: Long, pollResult: Byte,)
}
interface UniffiForeignFutureFree: Callback {
    fun callback(handle: Long,)
}
interface UniffiCallbackInterfaceFree: Callback {
    fun callback(handle: Long,)
}
