
private class UniffiJnaCleaner : UniffiCleaner {
    private val cleaner = com.sun.jna.internal.Cleaner.getCleaner()

    override fun register(value: Any, cleanUpTask: Runnable): UniffiCleaner.Cleanable =
        UniffiJnaCleanable(cleaner.register(value, cleanUpTask))
}

private class UniffiJnaCleanable(
    private val cleanable: com.sun.jna.internal.Cleaner.Cleanable,
) : UniffiCleaner.Cleanable {
    override fun clean() = cleanable.clean()
}

private fun UniffiCleaner.Companion.create(): UniffiCleaner = UniffiJnaCleaner()

actual fun runGC() {
    System.gc()
}