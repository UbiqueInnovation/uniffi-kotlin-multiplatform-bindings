internal object FfiConverterByteArray: FfiConverterRustBuffer<kotlin.ByteArray> {
    override fun read(buf: NoCopySource): kotlin.ByteArray {
        val len = buf.readInt()
        return buf.readByteArray(len.toLong())
    }
    override fun allocationSize(value: kotlin.ByteArray): Int {
        return 4 + value.size
    }
    override fun write(value: kotlin.ByteArray, buf: Buffer) {
        buf.writeInt(value.size)
        buf.write(value)
    }
}
