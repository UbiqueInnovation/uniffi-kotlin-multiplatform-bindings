
@kotlin.jvm.JvmInline
value class ByteBuffer(private val _buffer: okio.Buffer) {
    constructor() : this(okio.Buffer()) {}

    fun internal() = _buffer

    fun limit() = _buffer.size

    fun hasRemaining() = !_buffer.exhausted()

    

    fun get() = _buffer.readByte()

    fun get(bytesToRead: Long) = _buffer.readByteArray(bytesToRead)

    fun getShort() = _buffer.readShort()

    fun getInt() = _buffer.readInt()

    fun getLong() = _buffer.readLong()

    fun getFloat() = kotlin.Float.fromBits(_buffer.readInt())

    fun getDouble() = kotlin.Double.fromBits(_buffer.readLong())



    fun put(value: Byte) = _buffer.writeByte(value.toInt())

    fun put(src: ByteArray) = _buffer.write(src)

    fun putShort(value: Short) = _buffer.writeShort(value.toInt())

    fun putInt(value: Int) = _buffer.writeInt(value)

    fun putLong(value: Long) = _buffer.writeLong(value)

    fun putFloat(value: Float) = _buffer.writeInt(value.toRawBits())

    fun putDouble(value: Double) = _buffer.writeLong(value.toRawBits())


    private fun writeUtf8(value: String) = _buffer.writeUtf8(value)

    companion object {
        fun fromUtf8(value: String): ByteBuffer {
            // TODO: Figure out what happens when `value` is not valid UTF-8
            val buffer = ByteBuffer()
            buffer.writeUtf8(value)
            return buffer
        }
    }
}
