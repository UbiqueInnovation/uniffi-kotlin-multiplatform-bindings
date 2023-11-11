
// Map handles to objects
//
// This is used when the Rust code expects an opaque pointer to represent some foreign object.
// Normally we would pass a pointer to the object, but JNA doesn't support getting a pointer from an
// object reference , nor does it support leaking a reference to Rust.
//
// Instead, this class maps ULong values to objects so that we can pass a pointer-sized type to
// Rust when it needs an opaque pointer.
//
// TODO: refactor callbacks to use this class
expect class UniFfiHandleMap<T: Any>() {
    val size: Int
    fun insert(obj: T): ULong
    fun get(handle: ULong): T?
    fun remove(handle: ULong)
}
