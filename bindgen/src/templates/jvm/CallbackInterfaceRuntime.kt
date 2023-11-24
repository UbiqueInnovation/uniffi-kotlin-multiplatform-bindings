internal class NativeCallback(
    private val invokeImpl: (
        handle: Handle,
        method: kotlin.Int,
        argsData: UBytePointer,
        argsLen: kotlin.Int,
        outBuf: RustBufferByReference // RustBufferByReference
    ) -> kotlin.Int
) : com.sun.jna.Callback {
    fun invoke(
        handle: Handle,
        method: kotlin.Int,
        argsData: UBytePointer,
        argsLen: kotlin.Int,
        outBuf: RustBufferByReference // RustBufferByReference
    ): kotlin.Int = invokeImpl(handle, method, argsData, argsLen, outBuf)
}

internal actual typealias ForeignCallback = NativeCallback
