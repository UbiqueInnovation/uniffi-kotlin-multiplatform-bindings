object FfiConverterULong : FfiConverter<kotlin.ULong, kotlin.ULong> {
    override fun lift(value: kotlin.ULong): kotlin.ULong = value

    override fun read(source: NoCopySource): kotlin.ULong = lift(source.readLong().toULong())

    override fun lower(value: kotlin.ULong): kotlin.ULong = value

    override fun allocationSize(value: kotlin.ULong) = 8

    override fun write(value: kotlin.ULong, buf: Buffer) {
        buf.writeLong(value.toLong())
    }
}
