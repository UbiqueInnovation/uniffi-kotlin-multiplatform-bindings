{{ self.add_import("kotlin.time.Duration") }}
{{ self.add_import("kotlin.time.Duration.Companion.nanoseconds") }}
{{ self.add_import("kotlin.time.Duration.Companion.seconds") }}

object FfiConverterDuration : FfiConverterRustBuffer<Duration> {
    override fun read(source: NoCopySource): Duration {
        val seconds = source.readLong().seconds
        val nanoseconds = source.readInt().nanoseconds
        val duration = seconds + nanoseconds
        if (duration < 0.nanoseconds) {
            throw IllegalArgumentException("Duration nanoseconds exceed minimum or maximum supported by uniffi")
        }
        return duration
    }

    override fun allocationSize(value: Duration) = 12

    override fun write(value: Duration, buf: Buffer) {
        if (value < 0.nanoseconds) {
            throw IllegalArgumentException("Invalid duration, must be non-negative")
        }
        buf.writeLong(value.inWholeSeconds)
        buf.writeInt(value.inWholeNanoseconds.toInt())
    }
}