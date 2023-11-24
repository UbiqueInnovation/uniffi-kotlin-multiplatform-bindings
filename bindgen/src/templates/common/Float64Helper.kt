internal object FfiConverterDouble : FfiConverter<kotlin.Double, kotlin.Double> {
    override fun lift(value: kotlin.Double): kotlin.Double {
        return value
    }

    override fun read(buf: NoCopySource): kotlin.Double {
        return kotlin.Double.fromBits(buf.readLong())
    }

    override fun lower(value: kotlin.Double): kotlin.Double {
        return value
    }

    override fun allocationSize(value: kotlin.Double) = 8

    override fun write(value: kotlin.Double, buf: Buffer) {
        buf.writeLong(value.toRawBits())
    }
}
