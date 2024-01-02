internal object FfiConverterTimestamp : FfiConverterRustBuffer<kotlinx.datetime.Instant> {
    override fun read(buf: NoCopySource): kotlinx.datetime.Instant {
        val seconds = buf.readLong()
        val nanoseconds = buf.readInt()

        val instant = kotlinx.datetime.Instant.fromEpochSeconds(
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
    override fun allocationSize(value: kotlinx.datetime.Instant) = 12

    override fun write(value: kotlinx.datetime.Instant, buf: Buffer) {
        if (value.nanosecondsOfSecond < 0) {
            throw IllegalArgumentException("Invalid timestamp, nano value must be non-negative")
        }

        if (value.epochSeconds >= 0) {
            buf.writeLong(value.epochSeconds)
            buf.writeInt(value.nanosecondsOfSecond)
        } else {
            buf.writeLong(value.epochSeconds + 1)
            // UniFFI negates nanoseconds when epochSeconds is negative. See #37 for details.
            buf.writeInt(1_000_000_000 - value.nanosecondsOfSecond)
        }
    }
}
