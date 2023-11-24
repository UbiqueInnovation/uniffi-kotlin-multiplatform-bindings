internal object FfiConverterULong : FfiConverter<kotlin.ULong, kotlin.ULong> {
    override fun lift(value: kotlin.ULong): kotlin.ULong {
        return value
    }

    override fun read(buf: NoCopySource): kotlin.ULong {
        return lift(buf.readLong().toULong())
    }

    override fun lower(value: kotlin.ULong): kotlin.ULong {
        return value
    }

    override fun allocationSize(value: kotlin.ULong) = 8

    override fun write(value: kotlin.ULong, buf: Buffer) {
        buf.writeLong(value.toLong())
    }
}
