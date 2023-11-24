// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class Pointer

internal expect fun kotlin.Long.toPointer(): Pointer

internal expect fun Pointer.toLong(): kotlin.Long

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class UBytePointer

internal expect fun UBytePointer.asSource(len: kotlin.Long): NoCopySource

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class RustBuffer

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class RustBufferByReference

internal expect fun RustBuffer.asSource(): NoCopySource

internal expect val RustBuffer.dataSize: kotlin.Int

internal expect fun RustBuffer.free()

internal expect fun allocRustBuffer(buffer: Buffer): RustBuffer

internal expect fun RustBufferByReference.setValue(value: RustBuffer)

internal expect fun emptyRustBuffer(): RustBuffer

internal interface NoCopySource {
    fun exhausted(): kotlin.Boolean
    fun readByte(): kotlin.Byte
    fun readInt(): kotlin.Int
    fun readLong(): kotlin.Long
    fun readShort(): kotlin.Short
    fun readByteArray(): ByteArray
    fun readByteArray(len: kotlin.Long): ByteArray
}

// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class ForeignBytes
