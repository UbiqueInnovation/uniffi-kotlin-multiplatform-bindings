internal object FfiConverterString : FfiConverter<kotlin.String, RustBuffer> {
    // Note: we don't inherit from FfiConverterRustBuffer, because we use a
    // special encoding when lowering/lifting.  We can use `RustBuffer.len` to
    // store our length and avoid writing it out to the buffer.
    override fun lift(value: RustBuffer): kotlin.String {
        try {
            val byteArr = value.asSource().readByteArray(value.dataSize.toLong())
            return byteArr.decodeToString()
        } finally {
            value.free()
        }
    }

    override fun read(buf: NoCopySource): kotlin.String {
        val len = buf.readInt()
        val byteArr = buf.readByteArray(len.toLong())
        return byteArr.decodeToString()
    }

    override fun lower(value: kotlin.String): RustBuffer {
        val buffer = Buffer().write(value.encodeToByteArray())
        return allocRustBuffer(buffer)
    }

    // We aren't sure exactly how many bytes our string will be once it's UTF-8
    // encoded.  Allocate 3 bytes per UTF-16 code unit which will always be
    // enough.
    override fun allocationSize(value: kotlin.String): kotlin.Int {
        val sizeForLength = 4
        val sizeForString = value.length * 3
        return sizeForLength + sizeForString
    }

    override fun write(value: kotlin.String, buf: Buffer) {
        val byteArr = value.encodeToByteArray()
        buf.writeInt(byteArr.size)
        buf.write(byteArr)
    }
}
