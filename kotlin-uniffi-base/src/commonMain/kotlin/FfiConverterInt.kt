import okio.Buffer

object FfiConverterInt : FfiConverter<Int, Int> {
    override fun lift(value: Int): Int = value

    override fun read(source: NoCopySource): Int = source.readInt()

    override fun lower(value: Int): Int = value

    override fun allocationSize(value: Int) = 4

    override fun write(value: Int, buf: Buffer) {
        buf.writeInt(value)
    }
}
