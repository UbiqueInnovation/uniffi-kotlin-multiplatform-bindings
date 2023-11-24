{%- let cbi = ci|get_callback_interface_definition(name) %}
{%- let foreign_callback_name = format!("ForeignCallback{}", canonical_type_name) %}

{% if self.include_once_check("CallbackInterfaceRuntime.kt") %}{% include "CallbackInterfaceRuntime.kt" %}{% endif %}

{{- self.add_import("kotlinx.cinterop.staticCFunction") }}

internal actual fun {{ foreign_callback_name }}.toForeignCallback() : ForeignCallback =
    staticCFunction { handle: Handle, method: kotlin.Int, argsData: UBytePointer?, argLen: kotlin.Int, outBuf: RustBufferByReference? ->
        // *_Nonnull is ignored by cinterop
        {{ foreign_callback_name }}.callback(handle, method, requireNotNull(argsData), argLen, requireNotNull(outBuf))
    }
