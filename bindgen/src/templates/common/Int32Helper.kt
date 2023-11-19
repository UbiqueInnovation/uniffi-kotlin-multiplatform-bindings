object FfiConverterInt : FfiConverter<kotlin.Int, kotlin.Int> {
    override fun lift(value: kotlin.Int): kotlin.Int = value

    override fun read(source: NoCopySource): kotlin.Int = source.readInt()

    override fun lower(value: kotlin.Int): kotlin.Int = value

    override fun allocationSize(value: kotlin.Int) = 4

    override fun write(value: kotlin.Int, buf: Buffer) {
        buf.writeInt(value)
    }
}
