internal typealias GenericPointer = CPointer<out kotlinx.cinterop.CPointed>

actual class Pointer (val inner: GenericPointer) {
    actual constructor(value: Long) : this(requireNotNull(value.toCPointer()))
}

actual val NullPointer: Pointer? = null
actual fun getPointerNativeValue(ptr: Pointer): Long = ptr.inner.rawValue.toLong()
actual fun kotlin.Long.toPointer(): Pointer = Pointer(requireNotNull(this.toCPointer()))
