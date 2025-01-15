
 actual class UniffiRustCallStatus(val inner: CPointer<{{ ci.namespace() }}.cinterop.UniffiRustCallStatus>) {}
 actual var UniffiRustCallStatus.code: Byte
    get() = inner.pointed.code
    set(value) { inner.pointed.code = value }
 actual var UniffiRustCallStatus.error_buf: RustBufferByValue
    get() = RustBufferByValue(inner.pointed.errorBuf.readValue())
    set(value) { value.inner.place(inner.pointed.errorBuf.ptr) }

 actual class UniffiRustCallStatusByValue(val inner: CValue<{{ ci.namespace() }}.cinterop.UniffiRustCallStatus>) {}
 actual var UniffiRustCallStatusByValue.code: Byte
    get() = inner.useContents { code }
    set(value) {
        println("tried writing value")
    }
 actual var UniffiRustCallStatusByValue.error_buf: RustBufferByValue
    get() = inner.useContents { RustBufferByValue(errorBuf.readValue()) }
    set(value)  {
        println("tried writing value")
    }

 actual object UniffiRustCallStatusHelper
 actual fun UniffiRustCallStatusHelper.allocValue(): UniffiRustCallStatusByValue
    = UniffiRustCallStatusByValue(cValue<{{ ci.namespace() }}.cinterop.UniffiRustCallStatus>())
 actual fun <U> UniffiRustCallStatusHelper.withReference(
    block: (UniffiRustCallStatus) -> U
): U {
    return memScoped {
        val status = alloc<{{ ci.namespace() }}.cinterop.UniffiRustCallStatus>()
        block(UniffiRustCallStatus(status.ptr))
    }
}
