@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package uniffi.runtime

expect fun foo(): String

//////// POINTER ////////
expect class Pointer(value: Long)
expect val NullPointer: Pointer?
expect fun getPointerNativeValue(ptr: Pointer): Long
expect fun kotlin.Long.toPointer(): Pointer

//////// HELPERS ////////
class InternalException(message: String) : kotlin.Exception(message)

///////// TYPES /////////

// Interface implemented by anything that can contain an object reference.
//
// Such types expose a `destroy()` method that must be called to cleanly
// dispose of the contained objects. Failure to call this method may result
// in memory leaks.
//
// The easiest way to ensure this method is called is to use the `.use`
// helper method to execute a block and destroy the object at the end.
interface Disposable : AutoCloseable {
    fun destroy()
    override fun close() = destroy()
    companion object {
        fun destroy(vararg args: Any?) {
            args.filterIsInstance<Disposable>()
                .forEach(Disposable::destroy)
        }
    }
}

inline fun <T : Disposable?, R> T.use(block: (T) -> R) =
    try {
        block(this)
    } finally {
        try {
            // N.B. our implementation is on the nullable type `Disposable?`.
            this?.destroy()
        } catch (e: Throwable) {
            // swallow
        }
    }

/** Used to instantiate an interface without an actual pointer, for fakes in tests, mostly. */
object NoPointer