package uniffi.runtime

import kotlinx.datetime.Instant

object FfiConverterTimestamp: FfiConverterRustBuffer<Instant> {
    override fun read(buf: ByteBuffer): Instant {
        val seconds = buf.getLong()
        val nanoseconds = buf.getInt()

        val instant = Instant.fromEpochSeconds(
            seconds,
            // UniFFI negates nanoseconds when epochSeconds is negative. See #37 for details.
            if (seconds >= 0) nanoseconds else -nanoseconds,
        )
        if (nanoseconds < 0) {
            throw IllegalArgumentException("Instant nanoseconds exceed minimum or maximum supported by uniffi")
        }
        return instant
    }

    // 8 bytes for seconds, 4 bytes for nanoseconds
    override fun allocationSize(value: Instant) = 12UL

    override fun write(value: Instant, buf: ByteBuffer) {
        if (value.nanosecondsOfSecond < 0) {
            throw IllegalArgumentException("Invalid timestamp, nano value must be non-negative")
        }

        if (value.epochSeconds >= 0) {
            buf.putLong(value.epochSeconds)
            buf.putInt(value.nanosecondsOfSecond)
        } else {
            buf.putLong(value.epochSeconds + 1)
            // UniFFI negates nanoseconds when epochSeconds is negative. See #37 for details.
            buf.putInt(1_000_000_000 - value.nanosecondsOfSecond)
        }
    }
}
