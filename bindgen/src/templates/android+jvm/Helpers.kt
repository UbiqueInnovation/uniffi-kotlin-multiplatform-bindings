{% include "ffi/Helpers.kt" %}

@Structure.FieldOrder("code", "errorBuf")
internal open class UniffiRustCallStatusStruct(
    code: Byte,
    errorBuf: RustBufferByValue,
) : Structure() {
    @JvmField internal var code: Byte = code
    @JvmField internal var errorBuf: RustBufferByValue = errorBuf

    constructor(): this(0.toByte(), RustBufferByValue())

    internal class ByValue(
        code: Byte,
        errorBuf: RustBufferByValue,
    ): UniffiRustCallStatusStruct(code, errorBuf), Structure.ByValue {
        constructor(): this(0.toByte(), RustBufferByValue())
    }
    internal class ByReference(
        code: Byte,
        errorBuf: RustBufferByValue,
    ): UniffiRustCallStatusStruct(code, errorBuf), Structure.ByReference {
        constructor(): this(0.toByte(), RustBufferByValue())
    }
}

internal typealias UniffiRustCallStatus = UniffiRustCallStatusStruct.ByReference
internal var UniffiRustCallStatus.code: Byte
    get() = this.code
    set(value) { this.code = value }
internal var UniffiRustCallStatus.errorBuf: RustBufferByValue
    get() = this.errorBuf
    set(value) { this.errorBuf = value }

internal typealias UniffiRustCallStatusByValue = UniffiRustCallStatusStruct.ByValue
internal val UniffiRustCallStatusByValue.code: Byte
    get() = this.code
internal val UniffiRustCallStatusByValue.errorBuf: RustBufferByValue
    get() = this.errorBuf

internal object UniffiRustCallStatusHelper
internal fun UniffiRustCallStatusHelper.allocValue(): UniffiRustCallStatusByValue
    = UniffiRustCallStatusByValue()
internal fun <U> UniffiRustCallStatusHelper.withReference(
    block: (UniffiRustCallStatus) -> U
): U {
    val status = UniffiRustCallStatus()
    return block(status)
}
