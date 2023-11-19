object FfiConverterLong : FfiConverter<kotlin.Long, kotlin.Long> {
    override fun lift(value: kotlin.Long): kotlin.Long = value

    override fun read(source: NoCopySource): kotlin.Long = source.readLong()

    override fun lower(value: kotlin.Long): kotlin.Long = value

    override fun allocationSize(value: kotlin.Long) = 8

    override fun write(value: kotlin.Long, buf: Buffer) {
        buf.writeLong(value)
    }
}
