internal object FfiConverterBoolean : FfiConverter<kotlin.Boolean, kotlin.Byte> {
    override fun lift(value: kotlin.Byte): kotlin.Boolean = value.toInt() != 0

    override fun read(source: NoCopySource): kotlin.Boolean = lift(source.readByte())

    override fun lower(value: kotlin.Boolean): kotlin.Byte = if (value) 1.toByte() else 0.toByte()

    override fun allocationSize(value: kotlin.Boolean) = 1

    override fun write(value: kotlin.Boolean, buf: Buffer) {
        buf.writeByte(lower(value).toInt())
    }
}
