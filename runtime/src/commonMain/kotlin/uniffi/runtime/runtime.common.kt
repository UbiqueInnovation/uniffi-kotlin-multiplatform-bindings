@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package uniffi.runtime

//////// POINTER ////////
expect class Pointer(value: Long)
expect val NullPointer: Pointer?
expect fun getPointerNativeValue(ptr: Pointer): Long
expect fun kotlin.Long.toPointer(): Pointer

//////// HELPERS ////////
class InternalException(message: String) : kotlin.Exception(message)
