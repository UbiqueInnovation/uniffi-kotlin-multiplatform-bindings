object FfiConverterUInt : FfiConverter<kotlin.UInt, kotlin.UInt> {
    override fun lift(value: kotlin.UInt): kotlin.UInt = value

    override fun read(source: NoCopySource): kotlin.UInt = lift(source.readInt().toUInt())

    override fun lower(value: kotlin.UInt): kotlin.UInt = value

    override fun allocationSize(value: kotlin.UInt) = 4

    override fun write(value: kotlin.UInt, buf: Buffer) {
        buf.writeInt(value.toInt())
    }
}
