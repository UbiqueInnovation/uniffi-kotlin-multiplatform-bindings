import java.nio.ByteOrder

actual typealias Pointer = com.sun.jna.Pointer

actual fun Long.toPointer() = com.sun.jna.Pointer(this)

actual fun Pointer.toLong(): Long = com.sun.jna.Pointer.nativeValue(this)

actual typealias UBytePointer = com.sun.jna.Pointer

actual fun UBytePointer.asSource(len: Long): NoCopySource = object : NoCopySource {
    val buffer = getByteBuffer(0, len).also {
        it.order(ByteOrder.BIG_ENDIAN)
    }

    override fun exhausted(): Boolean = !buffer.hasRemaining()

    override fun readByte(): Byte = buffer.get()

    override fun readInt(): Int = buffer.getInt()

    override fun readLong(): Long = buffer.getLong()

    override fun readShort(): Short = buffer.getShort()

    override fun readByteArray(): ByteArray {
        val remaining = buffer.remaining()
        return readByteArray(remaining.toLong())
    }

    override fun readByteArray(len: Long): ByteArray {
        val startIndex = buffer.position().toLong()
        val indexAfterLast = (startIndex + len).toInt()
        val byteArray = getByteArray(startIndex, len.toInt())
        buffer.position(indexAfterLast)
        return byteArray
    }
}
