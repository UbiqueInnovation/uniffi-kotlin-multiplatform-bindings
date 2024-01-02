{{ self.add_import("kotlin.time.Duration.Companion.nanoseconds") }}
{{ self.add_import("kotlin.time.Duration.Companion.seconds") }}

internal object FfiConverterDuration : FfiConverterRustBuffer<kotlin.time.Duration> {
    override fun read(buf: NoCopySource): kotlin.time.Duration {
        val seconds = buf.readLong().seconds
        val nanoseconds = buf.readInt().nanoseconds
        val duration = seconds + nanoseconds
        if (duration < 0.nanoseconds) {
            throw IllegalArgumentException("Duration nanoseconds exceed minimum or maximum supported by uniffi")
        }
        return duration
    }

    override fun allocationSize(value: kotlin.time.Duration) = 12

    override fun write(value: kotlin.time.Duration, buf: Buffer) {
        if (value < 0.nanoseconds) {
            throw IllegalArgumentException("Invalid duration, must be non-negative")
        }
        value.toComponents { seconds, nanoseconds ->
            buf.writeLong(seconds)
            buf.writeInt(nanoseconds)
        }
    }
}
