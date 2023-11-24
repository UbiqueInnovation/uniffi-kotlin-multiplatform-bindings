// Suppressing the diagnostics caused by https://youtrack.jetbrains.com/issue/KT-37316
@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual typealias Pointer = com.sun.jna.Pointer

internal actual fun kotlin.Long.toPointer() = com.sun.jna.Pointer(this)

internal actual fun Pointer.toLong(): kotlin.Long = com.sun.jna.Pointer.nativeValue(this)

// Suppressing the diagnostics caused by https://youtrack.jetbrains.com/issue/KT-37316
@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual typealias UBytePointer = com.sun.jna.Pointer

internal actual fun UBytePointer.asSource(len: kotlin.Long): NoCopySource = object : NoCopySource {
    val buffer = getByteBuffer(0, len).also {
        it.order(java.nio.ByteOrder.BIG_ENDIAN)
    }

    override fun exhausted(): kotlin.Boolean = !buffer.hasRemaining()

    override fun readByte(): kotlin.Byte = buffer.get()

    override fun readInt(): kotlin.Int = buffer.getInt()

    override fun readLong(): kotlin.Long = buffer.getLong()

    override fun readShort(): kotlin.Short = buffer.getShort()

    override fun readByteArray(): ByteArray {
        val remaining = buffer.remaining()
        return readByteArray(remaining.toLong())
    }

    override fun readByteArray(len: kotlin.Long): ByteArray {
        val startIndex = buffer.position().toLong()
        val indexAfterLast = (startIndex + len).toInt()
        val byteArray = getByteArray(startIndex, len.toInt())
        buffer.position(indexAfterLast)
        return byteArray
    }
}

@com.sun.jna.Structure.FieldOrder("capacity", "len", "data")
internal open class RustBufferStructure : com.sun.jna.Structure() {
    @JvmField var capacity: kotlin.Int = 0
    @JvmField var len: kotlin.Int = 0
    @JvmField var data: com.sun.jna.Pointer? = null
}

internal actual open class RustBuffer : RustBufferStructure(), com.sun.jna.Structure.ByValue

internal actual class RustBufferByReference : com.sun.jna.ptr.ByReference(16) {
    fun setValueInternal(value: RustBuffer) {
        pointer.setInt(0, value.capacity)
        pointer.setInt(4, value.len)
        pointer.setPointer(8, value.data)
    }
}

internal actual fun RustBuffer.asSource(): NoCopySource = requireNotNull(data).asSource(len.toLong())

internal actual val RustBuffer.dataSize: kotlin.Int
    get() = len

internal actual fun RustBuffer.free() =
    rustCall { status: {{ config.package_name() }}.RustCallStatus ->
        UniFFILib.{{ ci.ffi_rustbuffer_free().name() }}(this, status)
    }

internal actual fun allocRustBuffer(buffer: Buffer): RustBuffer =
    rustCall { status: {{ config.package_name() }}.RustCallStatus ->
        val size = buffer.size
        var readPosition = 0L
        UniFFILib.{{ ci.ffi_rustbuffer_alloc().name() }}(size.toInt(), status).also { rustBuffer: RustBuffer ->
            val data = rustBuffer.data
                ?: throw RuntimeException("RustBuffer.alloc() returned null data pointer (size=${size})")
            rustBuffer.writeField("len", size.toInt())
            // Loop until the buffer is completed read, okio reads max 8192 bytes
            while (readPosition < size) {
                readPosition += buffer.read(data.getByteBuffer(readPosition, size - readPosition))
            }
        }
    }

internal actual fun RustBufferByReference.setValue(value: RustBuffer) = setValueInternal(value)

internal actual fun emptyRustBuffer(): RustBuffer = RustBuffer()

// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.

@com.sun.jna.Structure.FieldOrder("len", "data")
internal actual open class ForeignBytes : com.sun.jna.Structure() {
    @JvmField var len: kotlin.Int = 0
    @JvmField var data: com.sun.jna.Pointer? = null
}
