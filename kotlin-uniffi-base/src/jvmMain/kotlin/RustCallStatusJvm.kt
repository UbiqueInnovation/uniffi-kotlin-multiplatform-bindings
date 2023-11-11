import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.ByValue

@Structure.FieldOrder("code", "error_buf")
actual open class RustCallStatus : Structure() {
    @JvmField
    var code: Byte = 0

    @JvmField
    var error_buf: RustBuffer = RustBuffer()
}

actual val RustCallStatus.statusCode: Byte
    get() = code
actual val RustCallStatus.errorBuffer: RustBuffer
    get() = error_buf

actual fun <T> withRustCallStatus(block: (RustCallStatus) -> T): T {
    val rustCallStatus = RustCallStatus()
    return block(rustCallStatus)
}

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
actual open class RustCallStatusByValue : RustCallStatus(), ByValue
