{{ self.add_import("kotlin.time.Duration.Companion.nanoseconds") }}
{{ self.add_import("kotlin.time.Duration.Companion.seconds") }}

public object FfiConverterDuration: FfiConverterRustBuffer<kotlin.time.Duration> {
    override fun read(buf: ByteBuffer): kotlin.time.Duration {
        // Type mismatch (should be u64) but we check for overflow/underflow below
        val secs = buf.getLong()
        // Type mismatch (should be u32) but we check for overflow/underflow below
        val nanos = buf.getInt().toLong()
        if (secs < 0.seconds) {
            throw IllegalArgumentException("Duration exceeds minimum or maximum value supported by uniffi")
        }
        if (nanos < 0.nanoseconds) {
            throw IllegalArgumentException("Duration nanoseconds exceed minimum or maximum supported by uniffi")
        }
        return secs.seconds + nanos.nanoseconds
    }

    // 8 bytes for seconds, 4 bytes for nanoseconds
    override fun allocationSize(value: kotlin.time.Duration) = 12UL

    override fun write(value: kotlin.time.Duration, buf: ByteBuffer) {
        if (value.seconds < 0.seconds) {
            // Rust does not support negative Durations
            throw IllegalArgumentException("Invalid duration, must be non-negative")
        }

        if (value.nano < 0.nanoseconds) {
            throw IllegalArgumentException("Invalid duration, nano value must be non-negative")
        }

        // Type mismatch (should be u64) but since Rust doesn't support negative durations we should be OK
        buf.putLong(value.seconds)
        // Type mismatch (should be u32) but since values will always be between 0 and 999,999,999 it should be OK
        buf.putInt(value.nano)
    }
}
