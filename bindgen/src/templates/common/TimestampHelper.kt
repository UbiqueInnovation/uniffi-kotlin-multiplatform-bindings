// NOTE: write / read implementation differ from both `uniffi-rs` and `uniffi-kotlin-multiplatform-bindings`
// TODO: verify that everything works correctly!
public object FfiConverterTimestamp: FfiConverterRustBuffer<kotlinx.datetime.Instant> {
    override fun read(buf: ByteBuffer): kotlinx.datetime.Instant {
        val seconds = buf.getLong()
        // Type mismatch (should be u32) but we check for overflow/underflow below
        val nanoseconds = buf.getInt().toLong()
        if (nanoseconds < 0) {
            throw IllegalArgumentException("Instant nanoseconds exceed minimum or maximum supported by uniffi")
        }
        
        return kotlinx.datetime.Instant.fromEpochSeconds(
            seconds,
            nanoseconds
        )
    }

    // 8 bytes for seconds, 4 bytes for nanoseconds
    override fun allocationSize(value: kotlinx.datetime.Instant) = 12UL

    override fun write(value: kotlinx.datetime.Instant, buf: ByteBuffer) {
        if (value.nanosecondsOfSecond < 0) {
            throw IllegalArgumentException("Invalid timestamp, nano value must be non-negative")
        }

        buf.putLong(value.epochSeconds)
        // Type mismatch (should be u32) but since values will always be between 0 and 999,999,999 it should be OK
        buf.putInt(value.nanosecondsOfSecond)
    }
}
