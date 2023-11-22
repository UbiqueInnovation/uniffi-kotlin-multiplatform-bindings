internal object FfiConverterByteArray: FfiConverterRustBuffer<ByteArray> {
    override fun read(buf: NoCopySource): ByteArray {
        val len = buf.readInt()
        return buf.readByteArray(len.toLong())
    }
    override fun allocationSize(value: ByteArray): Int {
        return 4 + value.size
    }
    override fun write(value: ByteArray, buf: Buffer) {
        buf.writeInt(value.size)
        buf.write(value)
    }
}
