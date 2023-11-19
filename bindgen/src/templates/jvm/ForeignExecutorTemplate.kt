{{ self.add_import("kotlinx.coroutines.delay") }}
{{ self.add_import("kotlinx.coroutines.launch") }}

// TODO unify this code with the native source set? See the comment in the corresponding file
interface UniFfiRustTaskCallback: Callback {
    fun invoke(rustTaskData: Pointer?)
}

class UniFfiForeignExecutorCallbackImpl(
    private val invokeImpl: (
        handle: kotlin.ULong,
        delayMs: kotlin.Int,
        rustTask: UniFfiRustTaskCallback?,
        rustTaskData: Pointer?
    ) -> Unit
) : Callback {
    fun invoke(
        handle: kotlin.ULong,
        delayMs: kotlin.Int,
        rustTask: UniFfiRustTaskCallback?,
        rustTaskData: Pointer?
    ) = invokeImpl(handle, delayMs, rustTask, rustTaskData)
}

fun createUniFfiForeignExecutorCallbackImpl(
    block: (handle: kotlin.ULong, delayMs: kotlin.Int, rustTask: UniFfiRustTaskCallback?, rustTaskData: Pointer?) -> Unit
): UniFfiForeignExecutorCallback = UniFfiForeignExecutorCallbackImpl(block)

actual typealias UniFfiForeignExecutorCallback = UniFfiForeignExecutorCallbackImpl

fun invokeUniFfiForeignExecutorCallback(
    handle: kotlin.ULong,
    delayMs: kotlin.Int,
    rustTask: UniFfiRustTaskCallback?,
    rustTaskData: Pointer?
) {
    if (rustTask == null) {
        FfiConverterForeignExecutor.drop(handle)
    } else {
        val coroutineScope = FfiConverterForeignExecutor.lift(handle)
        coroutineScope.launch {
            if (delayMs > 0) {
                delay(delayMs.toLong())
            }
            rustTask.invoke(rustTaskData)
        }
    }
}

actual fun createUniFfiForeignExecutorCallback(): UniFfiForeignExecutorCallback =
    createUniFfiForeignExecutorCallbackImpl(::invokeUniFfiForeignExecutorCallback)
