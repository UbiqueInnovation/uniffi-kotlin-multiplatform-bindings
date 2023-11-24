internal object FfiConverterUShort : FfiConverter<kotlin.UShort, kotlin.UShort> {
    override fun lift(value: kotlin.UShort): kotlin.UShort {
        return value
    }

    override fun read(buf: NoCopySource): kotlin.UShort {
        return lift(buf.readShort().toUShort())
    }

    override fun lower(value: kotlin.UShort): kotlin.UShort {
        return value
    }

    override fun allocationSize(value: kotlin.UShort) = 2

    override fun write(value: kotlin.UShort, buf: Buffer) {
        buf.writeShort(value.toInt())
    }
}
