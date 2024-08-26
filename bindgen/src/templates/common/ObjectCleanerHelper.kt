{{ self.add_import("kotlinx.coroutines.Runnable") }}

// The cleaner interface for Object finalization code to run.
// This is the entry point to any implementation that we're using.
//
// The cleaner registers objects and returns cleanables, so now we are
// defining a `UniffiCleaner` with a `UniffiClenaer.Cleanable` to abstract the
// different implmentations available at compile time.
interface UniffiCleaner {
    interface Cleanable {
        fun clean()
    }

    fun register(value: Any, cleanUpTask: Runnable): UniffiCleaner.Cleanable

    companion object
}
