{%- let cbi = ci|get_callback_interface_definition(name) %}
{%- let foreign_callback_name = format!("ForeignCallback{}", name) %}

{% include "CallbackInterfaceImpl.kt" %}

{{- self.add_import("kotlinx.cinterop.staticCFunction") }}

actual fun {{ foreign_callback_name }}.toForeignCallback() : ForeignCallback =
    staticCFunction { handle: Handle, method: kotlin.Int, argsData: UBytePointer?, argLen: kotlin.Int, outBuf: RustBufferPointer? ->
        // *_Nonnull is ignored by cinterop
        {{ foreign_callback_name }}.invoke(handle, method, requireNotNull(argsData), argLen, requireNotNull(outBuf))
    }
