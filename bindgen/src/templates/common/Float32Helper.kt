internal object FfiConverterFloat : FfiConverter<kotlin.Float, kotlin.Float> {
    override fun lift(value: kotlin.Float): kotlin.Float {
        return value
    }

    override fun read(buf: NoCopySource): kotlin.Float {
        return kotlin.Float.fromBits(buf.readInt())
    }

    override fun lower(value: kotlin.Float): kotlin.Float {
        return value
    }

    override fun allocationSize(value: kotlin.Float) = 4

    override fun write(value: kotlin.Float, buf: Buffer) {
        buf.writeInt(value.toRawBits())
    }
}
