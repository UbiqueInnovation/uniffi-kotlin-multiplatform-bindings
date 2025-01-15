
@Structure.FieldOrder("code", "error_buf")
 open class UniffiRustCallStatusStruct : Structure() {
    @JvmField  var code: Byte = 0
    @JvmField  var error_buf: RustBufferByValue = RustBufferByValue()

     class ByValue: UniffiRustCallStatusStruct(), Structure.ByValue
     class ByReference: UniffiRustCallStatusStruct(), Structure.ByReference
}

 actual typealias UniffiRustCallStatus = UniffiRustCallStatusStruct.ByReference
 actual var UniffiRustCallStatus.code: Byte
    get() = this.code
    set(value) { this.code = value }
 actual var UniffiRustCallStatus.error_buf: RustBufferByValue
    get() = this.error_buf
    set(value) { this.error_buf = value }

 actual typealias UniffiRustCallStatusByValue = UniffiRustCallStatusStruct.ByValue
 actual var UniffiRustCallStatusByValue.code: Byte
    get() = this.code
    set(value) { this.code = value }
 actual var UniffiRustCallStatusByValue.error_buf: RustBufferByValue
    get() = this.error_buf
    set(value) { this.error_buf = value }

 actual object UniffiRustCallStatusHelper
 actual fun UniffiRustCallStatusHelper.allocValue(): UniffiRustCallStatusByValue
    = UniffiRustCallStatusByValue()
 actual fun <U> UniffiRustCallStatusHelper.withReference(
    block: (UniffiRustCallStatus) -> U
): U {
    val status = UniffiRustCallStatus()
    return block(status)
}
