
 actual typealias Pointer = com.sun.jna.Pointer
actual val NullPointer: Pointer? = com.sun.jna.Pointer.NULL
actual fun getPointerNativeValue(ptr: Pointer): Long = Pointer.nativeValue(ptr)
actual fun kotlin.Long.toPointer() = com.sun.jna.Pointer(this)
