// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class Pointer

expect fun Long.toPointer(): Pointer

expect fun Pointer.toLong(): Long

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class UBytePointer

expect fun UBytePointer.asSource(len: Long): NoCopySource

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class RustBuffer

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class RustBufferPointer

expect fun RustBuffer.asSource(): NoCopySource

expect val RustBuffer.dataSize: Int

expect fun RustBuffer.free()

expect fun allocRustBuffer(buffer: Buffer): RustBuffer

expect fun RustBufferPointer.setValue(value: RustBuffer)

expect fun emptyRustBuffer(): RustBuffer

interface NoCopySource {
    fun exhausted(): Boolean
    fun readByte(): Byte
    fun readInt(): Int
    fun readLong(): Long
    fun readShort(): Short
    fun readByteArray(): ByteArray
    fun readByteArray(len: Long): ByteArray
}

// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class ForeignBytes
