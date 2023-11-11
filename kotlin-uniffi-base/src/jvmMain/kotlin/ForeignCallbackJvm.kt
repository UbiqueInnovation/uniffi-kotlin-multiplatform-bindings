import com.sun.jna.Callback

class NativeCallback(
    private val invokeImpl: (
        handle: Handle,
        method: Int,
        argsData: UBytePointer,
        argsLen: Int,
        outBuf: RustBufferPointer // RustBufferByReference
    ) -> Int
) : Callback {
    fun invoke(
        handle: Handle,
        method: Int,
        argsData: UBytePointer,
        argsLen: Int,
        outBuf: RustBufferPointer // RustBufferByReference
    ): Int = invokeImpl(handle, method, argsData, argsLen, outBuf)
}

actual typealias ForeignCallback = NativeCallback
