object FfiConverterByte : FfiConverter<kotlin.Byte, kotlin.Byte> {
    override fun lift(value: kotlin.Byte): kotlin.Byte = value

    override fun read(source: NoCopySource): kotlin.Byte = source.readByte()

    override fun lower(value: kotlin.Byte): kotlin.Byte = value

    override fun allocationSize(value: kotlin.Byte) = 1

    override fun write(value: kotlin.Byte, buf: Buffer) {
        buf.writeByte(value.toInt())
    }
}
