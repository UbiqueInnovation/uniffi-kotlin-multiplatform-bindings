internal object FfiConverterLong : FfiConverter<kotlin.Long, kotlin.Long> {
    override fun lift(value: kotlin.Long): kotlin.Long {
        return value
    }

    override fun read(buf: NoCopySource): kotlin.Long {
        return buf.readLong()
    }

    override fun lower(value: kotlin.Long): kotlin.Long {
        return value
    }

    override fun allocationSize(value: kotlin.Long) = 8

    override fun write(value: kotlin.Long, buf: Buffer) {
        buf.writeLong(value)
    }
}
