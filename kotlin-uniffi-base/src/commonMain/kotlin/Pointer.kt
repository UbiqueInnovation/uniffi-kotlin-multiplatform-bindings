// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class Pointer

expect fun Long.toPointer(): Pointer

expect fun Pointer.toLong(): Long

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class UBytePointer

expect fun UBytePointer.asSource(len: Long): NoCopySource

interface NoCopySource {
    fun exhausted(): Boolean
    fun readByte(): Byte
    fun readInt(): Int
    fun readLong(): Long
    fun readShort(): Short
    fun readByteArray(): ByteArray
    fun readByteArray(len: Long): ByteArray
}
