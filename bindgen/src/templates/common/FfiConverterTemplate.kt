// The FfiConverter interface handles converter types to and from the FFI
//
// All implementing objects should be public to support external types.  When a
// type is external we need to import it's FfiConverter.
internal interface FfiConverter<KotlinType, FfiType> {
    // Convert an FFI type to a Kotlin type
    fun lift(value: FfiType): KotlinType

    // Convert an Kotlin type to an FFI type
    fun lower(value: KotlinType): FfiType

    // Read a Kotlin type from a `NoCopySource`
    fun read(buf: NoCopySource): KotlinType

    // Calculate bytes to allocate when creating a `RustBuffer`
    //
    // This must return at least as many bytes as the write() function will
    // write. It can return more bytes than needed, for example when writing
    // Strings we can't know the exact bytes needed until we the UTF-8
    // encoding, so we pessimistically allocate the largest size possible (3
    // bytes per codepoint).  Allocating extra bytes is not really a big deal
    // because the `RustBuffer` is short-lived.
    fun allocationSize(value: KotlinType): Int

    // Write a Kotlin type to a `ByteBuffer`
    fun write(value: KotlinType, buf: Buffer)

    // Lower a value into a `RustBuffer`
    //
    // This method lowers a value into a `RustBuffer` rather than the normal
    // FfiType.  It's used by the callback interface code.  Callback interface
    // returns are always serialized into a `RustBuffer` regardless of their
    // normal FFI type.
    fun lowerIntoRustBuffer(value: KotlinType): RustBuffer {
        val buffer = Buffer().apply { write(value, buffer) }
        return allocRustBuffer(buffer)
    }

    // Lift a value from a `RustBuffer`.
    //
    // This here mostly because of the symmetry with `lowerIntoRustBuffer()`.
    // It's currently only used by the `FfiConverterRustBuffer` class below.
    fun liftFromRustBuffer(rbuf: RustBuffer): KotlinType {
        val byteBuf = rbuf.asSource()
        try {
            val item = read(byteBuf)
            if (!byteBuf.exhausted()) {
                throw RuntimeException("junk remaining in buffer after lifting, something is very wrong!!")
            }
            return item
        } finally {
            rbuf.free()
        }
    }
}

// FfiConverter that uses `RustBuffer` as the FfiType
internal interface FfiConverterRustBuffer<KotlinType> : FfiConverter<KotlinType, RustBuffer> {
    override fun lift(value: RustBuffer) = liftFromRustBuffer(value)
    override fun lower(value: KotlinType) = lowerIntoRustBuffer(value)
}
