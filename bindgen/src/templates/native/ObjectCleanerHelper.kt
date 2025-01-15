
{{- self.add_import("kotlinx.atomicfu.atomic") }}
{{- self.add_import("kotlinx.atomicfu.AtomicBoolean") }}
{{- self.add_import("kotlinx.coroutines.Runnable") }}
{{- self.add_import("kotlin.native.ref.createCleaner") }}

private class NativeCleaner : UniffiCleaner {
    override fun register(value: Any, cleanUpTask: Runnable): UniffiCleaner.Cleanable =
        UniffiNativeCleanable(OnceRunnable(cleanUpTask))
}

private class UniffiNativeCleanable(val cleanUpTask: Runnable) : UniffiCleaner.Cleanable {
    @OptIn(ExperimentalNativeApi::class)
    private val cleaner = createCleaner(cleanUpTask) {
        it.run()
    }
    
    override fun clean() {
        cleanUpTask.run()
    }
}

private class OnceRunnable(val cleanUpTask: Runnable): Runnable {
    private val cleaned: AtomicBoolean = atomic(false)

    override fun run() {
        if (cleaned.compareAndSet(false, true)) {
            cleanUpTask.run()
        }
    }
}

private fun UniffiCleaner.Companion.create(): UniffiCleaner =
    NativeCleaner()
