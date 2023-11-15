// The FfiConverter interface handles converter types to and from the FFI
//
// All implementing objects should be public to support external types.  When a
// type is external we need to import it's FfiConverter.
interface FfiConverter<KotlinType, FfiType> {

    fun lowerIntoRustBuffer(value: KotlinType): RustBuffer {
        val buffer = Buffer().apply { write(value, buffer) }
        return allocRustBuffer(buffer)
    }

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

    fun lift(value: FfiType): KotlinType
    fun lower(value: KotlinType): FfiType
    fun read(source: NoCopySource): KotlinType
    fun allocationSize(value: KotlinType): Int
    fun write(value: KotlinType, buf: Buffer)
}

interface FfiConverterRustBuffer<KotlinType> : FfiConverter<KotlinType, RustBuffer> {
    override fun lift(value: RustBuffer) = liftFromRustBuffer(value)
    override fun lower(value: KotlinType) = lowerIntoRustBuffer(value)
}
