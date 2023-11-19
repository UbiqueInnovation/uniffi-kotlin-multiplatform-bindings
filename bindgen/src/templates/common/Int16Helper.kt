object FfiConverterShort : FfiConverter<kotlin.Short, kotlin.Short> {
    override fun lift(value: kotlin.Short): kotlin.Short = value

    override fun read(source: NoCopySource): kotlin.Short = source.readShort()

    override fun lower(value: kotlin.Short): kotlin.Short = value

    override fun allocationSize(value: kotlin.Short) = 2

    override fun write(value: kotlin.Short, buf: Buffer) {
        buf.writeShort(value.toInt())
    }
}
