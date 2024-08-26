// A handful of classes and functions to support the generated data structures.
// This would be a good candidate for isolating in its own ffi-support lib.

internal actual typealias UniffiRustCallStatus = CPointer<{{ ci.namespace() }}.cinterop.UniffiRustCallStatus>
internal actual var UniffiRustCallStatus.code: Byte
    get() = pointed.code
    set(value) { pointed.code = value }
internal actual var UniffiRustCallStatus.error_buf: RustBufferByValue
    get() = pointed.errorBuf.readValue()
    set(value) { value.place(pointed.errorBuf.ptr) }

internal actual typealias UniffiRustCallStatusByValue = CValue<{{ ci.namespace() }}.cinterop.UniffiRustCallStatus>
internal actual var UniffiRustCallStatusByValue.code: Byte
    get() = useContents { code }
    set(value) {
        println("tried writing value")
    }
internal actual var UniffiRustCallStatusByValue.error_buf: RustBufferByValue
    get() = useContents { errorBuf.readValue() }
    set(value)  {
        println("tried writing value")
    }

internal actual object UniffiRustCallStatusHelper
internal actual fun UniffiRustCallStatusHelper.allocValue(): UniffiRustCallStatusByValue
    = cValue<{{ ci.namespace() }}.cinterop.UniffiRustCallStatus>()
internal actual fun <U> UniffiRustCallStatusHelper.withReference(
    block: (UniffiRustCallStatus) -> U
): U {
    return memScoped {
        val status = alloc<{{ ci.namespace() }}.cinterop.UniffiRustCallStatus>()
        block(status.ptr)
    }
}