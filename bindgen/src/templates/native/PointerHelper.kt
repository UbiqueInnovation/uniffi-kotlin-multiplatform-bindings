internal actual typealias Pointer = CPointer<out kotlinx.cinterop.CPointed>
actual val NullPointer: Pointer? = null
actual fun getPointerNativeValue(ptr: Pointer): Long = ptr.rawValue.toLong()
actual fun kotlin.Long.toPointer(): Pointer = requireNotNull(this.toCPointer())