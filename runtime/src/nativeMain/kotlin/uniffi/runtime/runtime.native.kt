@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:OptIn(ExperimentalForeignApi::class)

package uniffi.runtime

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toCPointer

actual fun foo() = "Native"

//////// POINTER ////////
internal typealias GenericPointer = CPointer<out CPointed>
actual class Pointer (val inner: GenericPointer) {
    actual constructor(value: Long) : this(requireNotNull(value.toCPointer()))
}
actual val NullPointer: Pointer? = null
actual fun getPointerNativeValue(ptr: Pointer): Long = ptr.inner.rawValue.toLong()
actual fun kotlin.Long.toPointer(): Pointer = Pointer(requireNotNull(this.toCPointer()))
