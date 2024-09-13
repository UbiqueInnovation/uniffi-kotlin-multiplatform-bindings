
{{- self.add_import("android.os.Build") }}
{{- self.add_import("androidx.annotation.RequiresApi") }}

// The fallback Jna cleaner, which is available for both Android, and the JVM.
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

// The SystemCleaner, available from API Level 33.
// Some API Level 33 OSes do not support using it, so we require API Level 34.
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private class AndroidSystemCleaner : UniffiCleaner {
    val cleaner = android.system.SystemCleaner.cleaner()

    override fun register(value: Any, cleanUpTask: Runnable): UniffiCleaner.Cleanable =
        AndroidSystemCleanable(cleaner.register(value, cleanUpTask))
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private class AndroidSystemCleanable(
    private val cleanable: java.lang.ref.Cleaner.Cleanable,
) : UniffiCleaner.Cleanable {
    override fun clean() = cleanable.clean()
}

fun UniffiCleaner.Companion.create(): UniffiCleaner =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        AndroidSystemCleaner()
    } else {
        UniffiJnaCleaner()
    }

actual fun runGC() {
    System.gc()
}
