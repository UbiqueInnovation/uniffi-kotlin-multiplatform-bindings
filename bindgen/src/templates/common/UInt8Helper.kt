internal object FfiConverterUByte : FfiConverter<kotlin.UByte, kotlin.UByte> {
    override fun lift(value: kotlin.UByte): kotlin.UByte {
        return value
    }

    override fun read(buf: NoCopySource): kotlin.UByte {
        return lift(buf.readByte().toUByte())
    }

    override fun lower(value: kotlin.UByte): kotlin.UByte {
        return value
    }

    override fun allocationSize(value: kotlin.UByte) = 1

    override fun write(value: kotlin.UByte, buf: Buffer) {
        buf.writeByte(value.toInt())
    }
}
