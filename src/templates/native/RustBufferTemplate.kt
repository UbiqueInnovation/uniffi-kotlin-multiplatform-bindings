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

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustBuffer = CValue<{{ config.package_name() }}.cinterop.RustBuffer>

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias RustBufferPointer = CPointer<{{ config.package_name() }}.cinterop.RustBuffer>

actual fun RustBuffer.asSource(): NoCopySource {
    val data = useContents { data }
    val len = useContents { len }
    return requireNotNull(data).asSource(len.toLong())
}

actual val RustBuffer.dataSize: Int
    get() = useContents { len }

actual fun RustBuffer.free(): Unit =
    rustCall { status ->
        UniFFILib.{{ ci.ffi_rustbuffer_free().name() }}(this, status)
    }

actual fun allocRustBuffer(buffer: Buffer): RustBuffer =
    rustCall { status ->
        val size = buffer.size
        UniFFILib.{{ ci.ffi_rustbuffer_alloc().name() }}(size.toInt(), status).also {
            it.useContents {
                val notNullData = data
                checkNotNull(notNullData) { "RustBuffer.alloc() returned null data pointer (size=${size})" }
                buffer.readByteArray().forEachIndexed { index, byte ->
                    notNullData[index] = byte.toUByte()
                }
            }
        }
    }

actual fun RustBufferPointer.setValue(value: RustBuffer) {
    this.pointed.capacity = value.useContents { capacity }
    this.pointed.len = value.useContents { len }
    this.pointed.data = value.useContents { data }
}

actual fun emptyRustBuffer(): RustBuffer {
    return allocRustBuffer(Buffer())
}

// This is a helper for safely passing byte references into the rust code.
// It's not actually used at the moment, because there aren't many things that you
// can take a direct pointer to in the JVM, and if we're going to copy something
// then we might as well copy it into a `RustBuffer`. But it's here for API
// completeness.

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias ForeignBytes = CValue<{{ config.package_name() }}.cinterop.ForeignBytes>
