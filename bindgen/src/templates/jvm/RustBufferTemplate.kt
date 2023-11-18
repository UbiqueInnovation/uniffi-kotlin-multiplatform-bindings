actual typealias Pointer = com.sun.jna.Pointer

actual fun Long.toPointer() = com.sun.jna.Pointer(this)

actual fun Pointer.toLong(): Long = com.sun.jna.Pointer.nativeValue(this)

actual typealias UBytePointer = com.sun.jna.Pointer

actual fun UBytePointer.asSource(len: Long): NoCopySource = object : NoCopySource {
    val buffer = getByteBuffer(0, len).also {
        it.order(java.nio.ByteOrder.BIG_ENDIAN)
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

@Structure.FieldOrder("capacity", "len", "data")
open class RustBufferStructure : Structure() {
    @JvmField
    var capacity: Int = 0

    @JvmField
    var len: Int = 0

    @JvmField
    var data: Pointer? = null
}

actual class RustBuffer : RustBufferStructure(), Structure.ByValue

actual class RustBufferPointer : ByReference(16) {
    fun setValueInternal(value: RustBuffer) {
        pointer.setInt(0, value.capacity)
        pointer.setInt(4, value.len)
        pointer.setPointer(8, value.data)
    }
}

actual fun RustBuffer.asSource(): NoCopySource = requireNotNull(data).asSource(len.toLong())

actual val RustBuffer.dataSize: Int
    get() = len

actual fun RustBuffer.free() =
    rustCall { status ->
        UniFFILib.{{ ci.ffi_rustbuffer_free().name() }}(this, status)
    }

actual fun allocRustBuffer(buffer: Buffer): RustBuffer =
    rustCall { status ->
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

actual fun RustBufferPointer.setValue(value: RustBuffer) = setValueInternal(value)

actual fun emptyRustBuffer(): RustBuffer = RustBuffer()

// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.

@Structure.FieldOrder("len", "data")
actual open class ForeignBytes : Structure() {
    @JvmField
    var len: Int = 0

    @JvmField
    var data: Pointer? = null
}
