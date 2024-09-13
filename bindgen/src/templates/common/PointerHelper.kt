
expect class Pointer(value: Long)
expect val NullPointer: Pointer?
expect fun getPointerNativeValue(ptr: Pointer): Long
expect fun kotlin.Long.toPointer(): Pointer
