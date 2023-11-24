internal object FfiConverterByte : FfiConverter<kotlin.Byte, kotlin.Byte> {
    override fun lift(value: kotlin.Byte): kotlin.Byte {
        return value
    }

    override fun read(buf: NoCopySource): kotlin.Byte {
        return buf.readByte()
    }

    override fun lower(value: kotlin.Byte): kotlin.Byte {
        return value
    }

    override fun allocationSize(value: kotlin.Byte) = 1

    override fun write(value: kotlin.Byte, buf: Buffer) {
        buf.writeByte(value.toInt())
    }
}
