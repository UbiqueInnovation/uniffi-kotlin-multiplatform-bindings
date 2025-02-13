package uniffi.runtime

import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Runnable
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner

// The cleaner interface for Object finalization code to run.
// This is the entry point to any implementation that we're using.
//
// The cleaner registers objects and returns cleanables, so now we are
// defining a `UniffiCleaner` with a `UniffiCleaner.Cleanable` to abstract the
// different implementations available at compile time.
interface UniffiCleaner {
    interface Cleanable {
        fun clean()
    }

    fun register(value: Any, cleanUpTask: Runnable): Cleanable

    companion object
}

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
