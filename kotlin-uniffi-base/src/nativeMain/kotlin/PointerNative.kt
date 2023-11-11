import kotlinx.cinterop.*
import okio.Buffer

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE")
actual typealias Pointer = CPointer<out CPointed>

actual fun Long.toPointer(): Pointer = requireNotNull(this.toCPointer())

actual fun Pointer.toLong(): Long = this.rawValue.toLong()

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias UBytePointer = CPointer<UByteVar>

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
internal inline infix fun Byte.and(other: Long): Long = toLong() and other

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
internal inline infix fun Byte.and(other: Int): Int = toInt() and other

// byte twiddling was basically pasted from okio
actual fun UBytePointer.asSource(len: Long): NoCopySource = object : NoCopySource {
    var readBytes: Int = 0
    var remaining: Long = len

    init {
        if (len < 0) {
            throw IllegalStateException("Trying to create NoCopySource with negative length")
        }
    }

    private fun requireLen(requiredLen: Long) {
        if (remaining < requiredLen) {
            throw IllegalStateException("Expected at least ${requiredLen} bytes in source but have only ${len}")
        }
        remaining -= requiredLen
    }

    override fun exhausted(): Boolean = remaining == 0L

    override fun readByte(): Byte {
        requireLen(1)
        return reinterpret<ByteVar>()[readBytes++]
    }

    override fun readShort(): Short {
        requireLen(2)
        val data = reinterpret<ByteVar>()
        val s = data[readBytes++] and 0xff shl 8 or (data[readBytes++] and 0xff)
        return s.toShort()
    }

    override fun readInt(): Int {
        requireLen(4)
        val data = reinterpret<ByteVar>()
        val i = (
                data[readBytes++] and 0xff shl 24
                        or (data[readBytes++] and 0xff shl 16)
                        or (data[readBytes++] and 0xff shl 8)
                        or (data[readBytes++] and 0xff)
                )
        return i
    }

    override fun readLong(): Long {
        requireLen(8)
        val data = reinterpret<ByteVar>()
        val v = (
                data[readBytes++] and 0xffL shl 56
                        or (data[readBytes++] and 0xffL shl 48)
                        or (data[readBytes++] and 0xffL shl 40)
                        or (data[readBytes++] and 0xffL shl 32)
                        or (data[readBytes++] and 0xffL shl 24)
                        or (data[readBytes++] and 0xffL shl 16)
                        or (data[readBytes++] and 0xffL shl 8) // ktlint-disable no-multi-spaces
                        or (data[readBytes++] and 0xffL)
                )
        return v
    }

    override fun readByteArray(): ByteArray = readByteArray(len)

    override fun readByteArray(len: Long): ByteArray {
        requireLen(len)

        val cast = reinterpret<ByteVar>()
        val intLen = len.toInt()
        val byteArray = ByteArray(intLen)

        for (writeIdx in 0 until intLen) {
            byteArray[writeIdx] = cast[readBytes++]
        }

        return byteArray
    }
}
