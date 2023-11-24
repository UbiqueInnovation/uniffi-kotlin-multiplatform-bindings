{{ self.add_import("kotlinx.coroutines.delay") }}
{{ self.add_import("kotlinx.coroutines.launch") }}

// TODO unify this code with the native source set? See the comment in the corresponding file
interface UniFfiRustTaskCallback: com.sun.jna.Callback {
    fun invoke(rustTaskData: Pointer?)
}

class UniFfiForeignExecutorCallbackImpl(
    private val invokeImpl: (
        handle: kotlin.ULong,
        delayMs: kotlin.Int,
        rustTask: UniFfiRustTaskCallback?,
        rustTaskData: com.sun.jna.Pointer?
    ) -> Unit
) : com.sun.jna.Callback {
    fun invoke(
        handle: kotlin.ULong,
        delayMs: kotlin.Int,
        rustTask: UniFfiRustTaskCallback?,
        rustTaskData: com.sun.jna.Pointer?
    ) = invokeImpl(handle, delayMs, rustTask, rustTaskData)
}

fun createUniFfiForeignExecutorCallbackImpl(
    block: (handle: kotlin.ULong, delayMs: kotlin.Int, rustTask: UniFfiRustTaskCallback?, rustTaskData: com.sun.jna.Pointer?) -> Unit
): UniFfiForeignExecutorCallback = UniFfiForeignExecutorCallbackImpl(block)

actual typealias UniFfiForeignExecutorCallback = UniFfiForeignExecutorCallbackImpl

fun invokeUniFfiForeignExecutorCallback(
    handle: kotlin.ULong,
    delayMs: kotlin.Int,
    rustTask: UniFfiRustTaskCallback?,
    rustTaskData: com.sun.jna.Pointer?
): kotlin.Byte {
    if (rustTask == null) {
        FfiConverterForeignExecutor.drop(handle)
        return UNIFFI_FOREIGN_EXECUTOR_CALLBACK_SUCCESS
    } else {
        val coroutineScope = FfiConverterForeignExecutor.lift(handle)
        if (coroutineScope.isActive) {
            coroutineScope.launch {
                val job = coroutineScope.launch {
                    if (delayMs > 0) {
                        delay(delayMs.toLong())
                    }
                    rustTask.callback(rustTaskData, UNIFFI_RUST_TASK_CALLBACK_SUCCESS)
                }
                job.invokeOnCompletion { cause ->
                    if (cause != null) {
                        rustTask.callback(rustTaskData, UNIFFI_RUST_TASK_CALLBACK_CANCELLED)
                    }
                }
                return UNIFFI_FOREIGN_EXECUTOR_CALLBACK_SUCCESS
            } else {
                return UNIFFI_FOREIGN_EXECUTOR_CALLBACK_CANCELLED
            }
        }
    }
}

actual fun createUniFfiForeignExecutorCallback(): UniFfiForeignExecutorCallback =
    createUniFfiForeignExecutorCallbackImpl(::invokeUniFfiForeignExecutorCallback)
