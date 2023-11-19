object FfiConverterUByte : FfiConverter<kotlin.UByte, kotlin.UByte> {
    override fun lift(value: kotlin.UByte): kotlin.UByte = value

    override fun read(source: NoCopySource): kotlin.UByte = lift(source.readByte().toUByte())

    override fun lower(value: kotlin.UByte): kotlin.UByte = value

    override fun allocationSize(value: kotlin.UByte) = 1

    override fun write(value: kotlin.UByte, buf: Buffer) {
        buf.writeByte(value.toInt())
    }
}
