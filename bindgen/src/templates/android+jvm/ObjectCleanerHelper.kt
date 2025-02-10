{% include "ffi/ObjectCleanerHelper.kt" %}
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

{%- if config.disable_java_cleaner %}
private fun UniffiCleaner.Companion.create(): UniffiCleaner = UniffiJnaCleaner()
{%- else if module_name == "android" %}
{{- self.add_import("android.os.Build") }}
{{- self.add_import("androidx.annotation.RequiresApi") }}

// The SystemCleaner, available from API Level 33.
// Some API Level 33 OSes do not support using it, so we require API Level 34.
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private class AndroidSystemCleaner : UniffiCleaner {
    private val cleaner = android.system.SystemCleaner.cleaner()

    override fun register(value: Any, cleanUpTask: Runnable): UniffiCleaner.Cleanable =
        AndroidSystemCleanable(cleaner.register(value, cleanUpTask))
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private class AndroidSystemCleanable(
    private val cleanable: java.lang.ref.Cleaner.Cleanable,
) : UniffiCleaner.Cleanable {
    override fun clean() = cleanable.clean()
}

private fun UniffiCleaner.Companion.create(): UniffiCleaner =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        AndroidSystemCleaner()
    } else {
        UniffiJnaCleaner()
    }

{%- else %}

private class JavaLangRefCleaner : UniffiCleaner {
    private val cleaner: java.lang.ref.Cleaner = java.lang.ref.Cleaner.create()

    override fun register(value: Any, cleanUpTask: Runnable): UniffiCleaner.Cleanable =
        JavaLangRefCleanable(cleaner.register(value, cleanUpTask))
}

private class JavaLangRefCleanable(
    val cleanable: java.lang.ref.Cleaner.Cleanable
) : UniffiCleaner.Cleanable {
    override fun clean() = cleanable.clean()
}

private fun UniffiCleaner.Companion.create(): UniffiCleaner =
    try {
        JavaLangRefCleaner()
    } catch (e: ClassNotFoundException) {
        UniffiJnaCleaner()
    }

{%- endif %}