class NativeCallback(
    private val invokeImpl: (
        handle: Handle,
        method: kotlin.Int,
        argsData: UBytePointer,
        argsLen: kotlin.Int,
        outBuf: RustBufferPointer // RustBufferByReference
    ) -> kotlin.Int
) : Callback {
    fun invoke(
        handle: Handle,
        method: kotlin.Int,
        argsData: UBytePointer,
        argsLen: kotlin.Int,
        outBuf: RustBufferPointer // RustBufferByReference
    ): kotlin.Int = invokeImpl(handle, method, argsData, argsLen, outBuf)
}

actual typealias ForeignCallback = NativeCallback
