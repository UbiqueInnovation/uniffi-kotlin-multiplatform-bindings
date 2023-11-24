internal object FfiConverterInt : FfiConverter<kotlin.Int, kotlin.Int> {
    override fun lift(value: kotlin.Int): kotlin.Int {
        return value
    }

    override fun read(buf: NoCopySource): kotlin.Int {
        return buf.readInt()
    }

    override fun lower(value: kotlin.Int): kotlin.Int {
        return value
    }

    override fun allocationSize(value: kotlin.Int) = 4

    override fun write(value: kotlin.Int, buf: Buffer) {
        buf.writeInt(value)
    }
}
