@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
actual fun runGC() {
    kotlin.native.runtime.GC.collect()
}
