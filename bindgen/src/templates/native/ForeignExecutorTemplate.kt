{{ self.add_import("kotlinx.cinterop.CFunction") }}
{{ self.add_import("kotlinx.cinterop.CPointer") }}
{{ self.add_import("kotlinx.cinterop.invoke") }}
{{ self.add_import("kotlinx.cinterop.staticCFunction") }}
{{ self.add_import("kotlinx.coroutines.delay") }}
{{ self.add_import("kotlinx.coroutines.launch") }}

typealias UniFfiRustTaskCallback = CPointer<CFunction<(Pointer?) -> Unit>>

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias UniFfiForeignExecutorCallback = CPointer<CFunction<(kotlin.ULong, kotlin.UInt, UniFfiRustTaskCallback?, Pointer?) -> Unit>>

// TODO find a way to unify this with the JVM source? The issue is that with JNA, UniFfiRustTaskCallback is (must be?)
//      an interface implementing jna.Callback. On Native, it is (must be?) a CPointer which is a class, not an interface
//      `expect typealias UniFfiRustTaskCallback` could be a solution but that is currently not allowed
fun invokeUniFfiForeignExecutorCallback(
    handle: kotlin.ULong,
    delayMs: kotlin.Int,
    rustTask: UniFfiRustTaskCallback?,
    rustTaskData: Pointer?
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
    staticCFunction { handle, delayMs, rustTask, rustTaskData ->
        // FIXME the rusty delayMs is an u32, so Int was probalby the wrong type all along?
        //       fix the java types so no negative values are created here
        invokeUniFfiForeignExecutorCallback(handle, delayMs.toInt(), rustTask, rustTaskData)
    }
