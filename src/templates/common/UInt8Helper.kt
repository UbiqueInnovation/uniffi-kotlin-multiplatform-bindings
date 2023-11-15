object FfiConverterUByte : FfiConverter<UByte, UByte> {
    override fun lift(value: UByte): UByte = value

    override fun read(source: NoCopySource): UByte = lift(source.readByte().toUByte())

    override fun lower(value: UByte): UByte = value

    override fun allocationSize(value: UByte) = 1

    override fun write(value: UByte, buf: Buffer) {
        buf.writeByte(value.toInt())
    }
}