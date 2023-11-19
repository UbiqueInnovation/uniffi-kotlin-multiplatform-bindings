object FfiConverterInstant : FfiConverterRustBuffer<kotlinx.datetime.Instant> {
    override fun read(source: NoCopySource): kotlinx.datetime.Instant {
        val seconds = source.readLong()
        val nanoseconds = source.readInt()
        val instant = kotlinx.datetime.Instant.fromEpochSeconds(seconds, nanoseconds)
        if (nanoseconds < 0) {
            throw IllegalArgumentException("Instant nanoseconds exceed minimum or maximum supported by uniffi")
        }
        return instant
    }

    override fun allocationSize(value: kotlinx.datetime.Instant) = 12

    override fun write(value: kotlinx.datetime.Instant, buf: Buffer) {
        value.epochSeconds

        if (value.nanosecondsOfSecond < 0) {
            throw IllegalArgumentException("Invalid timestamp, nano value must be non-negative")
        }

        buf.writeLong(value.epochSeconds)
        buf.writeInt(value.nanosecondsOfSecond)
    }
}