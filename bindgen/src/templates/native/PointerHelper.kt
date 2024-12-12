actual class Pointer(val inner: CPointer<out kotlinx.cinterop.CPointed>) {
}

actual val NullPointer: Pointer? = null
actual fun getPointerNativeValue(ptr: Pointer): Long = ptr.inner.rawValue.toLong()
actual fun kotlin.Long.toPointer(): Pointer = Pointer(requireNotNull(this.toCPointer()))
