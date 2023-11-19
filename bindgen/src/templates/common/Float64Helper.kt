object FfiConverterDouble : FfiConverter<kotlin.Double, kotlin.Double> {
    override fun lift(value: kotlin.Double): kotlin.Double = value

    override fun read(source: NoCopySource): kotlin.Double = kotlin.Double.fromBits(source.readLong())

    override fun lower(value: kotlin.Double): kotlin.Double = value

    override fun allocationSize(value: kotlin.Double) = 8

    override fun write(value: kotlin.Double, buf: Buffer) {
        buf.writeLong(value.toRawBits())
    }
}
