import okio.Buffer

object FfiConverterByte : FfiConverter<Byte, Byte> {
    override fun lift(value: Byte): Byte = value

    override fun read(source: NoCopySource): Byte = source.readByte()

    override fun lower(value: Byte): Byte = value

    override fun allocationSize(value: Byte) = 1

    override fun write(value: Byte, buf: Buffer) {
        buf.writeByte(value.toInt())
    }
}
