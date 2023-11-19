object FfiConverterFloat : FfiConverter<kotlin.Float, kotlin.Float> {
    override fun lift(value: kotlin.Float): kotlin.Float = value

    override fun read(source: NoCopySource): kotlin.Float = kotlin.Float.fromBits(source.readInt())

    override fun lower(value: kotlin.Float): kotlin.Float = value

    override fun allocationSize(value: kotlin.Float) = 4

    override fun write(value: kotlin.Float, buf: Buffer) {
        buf.writeInt(value.toRawBits())
    }
}
