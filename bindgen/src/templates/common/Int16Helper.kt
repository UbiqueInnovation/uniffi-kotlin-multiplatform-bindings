internal object FfiConverterShort : FfiConverter<kotlin.Short, kotlin.Short> {
    override fun lift(value: kotlin.Short): kotlin.Short {
        return value
    }

    override fun read(buf: NoCopySource): kotlin.Short {
        return buf.readShort()
    }

    override fun lower(value: kotlin.Short): kotlin.Short {
        return value
    }

    override fun allocationSize(value: kotlin.Short) = 2

    override fun write(value: kotlin.Short, buf: Buffer) {
        buf.writeShort(value.toInt())
    }
}
