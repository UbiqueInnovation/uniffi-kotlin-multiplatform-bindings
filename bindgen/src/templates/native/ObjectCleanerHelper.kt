
{{- self.add_import("kotlinx.coroutines.Runnable") }}
{{- self.add_import("kotlin.native.ref.createCleaner") }}

private class NativeCleaner : UniffiCleaner {
    override fun register(value: Any, cleanUpTask: Runnable): UniffiCleaner.Cleanable =
        UniffiNativeCleanable(value, cleanUpTask)
}

private class UniffiNativeCleanable(val value: Any, val cleanUpTask: Runnable) : UniffiCleaner.Cleanable {
    @OptIn(ExperimentalNativeApi::class)
    override fun clean() {
        createCleaner(this) {
            it.cleanUpTask.run()
        }
    }
}

private fun UniffiCleaner.Companion.create(): UniffiCleaner =
    NativeCleaner()
