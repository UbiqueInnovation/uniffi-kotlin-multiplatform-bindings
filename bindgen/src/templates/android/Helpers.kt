// A handful of classes and functions to support the generated data structures.
// This would be a good candidate for isolating in its own ffi-support lib.

@Structure.FieldOrder("code", "error_buf")
internal open class UniffiRustCallStatusStruct : Structure() {
    @JvmField internal var code: Byte = 0
    @JvmField internal var error_buf: RustBufferByValue = RustBufferByValue()

    internal class ByValue: UniffiRustCallStatusStruct(), Structure.ByValue
    internal class ByReference: UniffiRustCallStatusStruct(), Structure.ByReference
}

internal actual typealias UniffiRustCallStatus = UniffiRustCallStatusStruct.ByReference
internal actual var UniffiRustCallStatus.code: Byte
    get() = this.code
    set(value) { this.code = value }
internal actual var UniffiRustCallStatus.error_buf: RustBufferByValue
    get() = this.error_buf
    set(value) { this.error_buf = value }

internal actual typealias UniffiRustCallStatusByValue = UniffiRustCallStatusStruct.ByValue
internal actual var UniffiRustCallStatusByValue.code: Byte
    get() = this.code
    set(value) { this.code = value }
internal actual var UniffiRustCallStatusByValue.error_buf: RustBufferByValue
    get() = this.error_buf
    set(value) { this.error_buf = value }

internal actual object UniffiRustCallStatusHelper
internal actual fun UniffiRustCallStatusHelper.allocValue(): UniffiRustCallStatusByValue
    = UniffiRustCallStatusByValue()
internal actual fun <U> UniffiRustCallStatusHelper.withReference(
    block: (UniffiRustCallStatus) -> U
): U {
    val status = UniffiRustCallStatus()
    return block(status)
}