{% include "ffi/ObjectCleanerHelper.kt" %}
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

private class OnceRunnable(val task: Runnable): Runnable {
    private val didRun: AtomicBoolean = atomic(false)

    override fun run() {
        if (didRun.compareAndSet(false, true)) {
            task.run()
        }
    }
}

private fun UniffiCleaner.Companion.create(): UniffiCleaner =
    NativeCleaner()
