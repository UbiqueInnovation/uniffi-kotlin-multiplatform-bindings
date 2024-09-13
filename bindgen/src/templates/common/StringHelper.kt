
public object FfiConverterString: FfiConverter<String, RustBufferByValue> {
    // Note: we don't inherit from FfiConverterRustBuffer, because we use a
    // special encoding when lowering/lifting.  We can use `RustBuffer.len` to
    // store our length and avoid writing it out to the buffer.
    override fun lift(value: RustBufferByValue): String {
        try {
            val byteArr =  value.asByteBuffer()!!.get(value.len)
            return byteArr.decodeToString()
        } finally {
            RustBufferHelper.free(value)
        }
    }

    override fun read(buf: ByteBuffer): String {
        val len = buf.getInt()
        val byteArr = buf.get(len.toLong())
        return byteArr.decodeToString()
    }

    fun toUtf8(value: String): ByteBuffer {
        return ByteBuffer.fromUtf8(value)
    }

    override fun lower(value: String): RustBufferByValue {
        val byteBuf = toUtf8(value)
        return RustBufferHelper.allocFromByteBuffer(byteBuf)
    }

    // We aren't sure exactly how many bytes our string will be once it's UTF-8
    // encoded.  Allocate 3 bytes per UTF-16 code unit which will always be
    // enough.
    override fun allocationSize(value: String): ULong {
        val sizeForLength = 4UL
        val sizeForString = value.length.toULong() * 3UL
        return sizeForLength + sizeForString
    }

    override fun write(value: String, buf: ByteBuffer) {
        // TODO: solve this more cleanly
        val byteBuf = toUtf8(value)
        buf.putInt(byteBuf.limit().toInt())
        buf.internal().writeUtf8(value)
    }
}
