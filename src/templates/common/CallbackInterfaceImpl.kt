{% if self.include_once_check("CallbackInterfaceRuntime.kt") %}{% include "CallbackInterfaceRuntime.kt" %}{% endif %}

object {{ foreign_callback_name }} {
    @Suppress("TooGenericExceptionCaught")
    fun invoke(handle: Handle, method: Int, argsData: UBytePointer, argsLen: Int, outBuf: RustBufferPointer): Int {
        val cb = {{ ffi_converter_name }}.lift(handle)
        return when (method) {
            IDX_CALLBACK_FREE -> {
                {{ ffi_converter_name }}.drop(handle)
                UNIFFI_CALLBACK_SUCCESS
            }
            {% for meth in cbi.methods() -%}
            {% let method_name = format!("invoke_{}", meth.name())|fn_name -%}
            {{ loop.index }} -> {
                try {
                    {%- match meth.throws_type() %}
                    {%- when Some(error_type) %}
                    try {
                        val buffer = this.{{ method_name }}(cb, argsData, argsLen)
                        outBuf.setValue(buffer)
                        UNIFFI_CALLBACK_SUCCESS
                    } catch (e: {{ error_type|error_type_name }}) {
                        val buffer = {{ error_type|error_ffi_converter_name }}.lowerIntoRustBuffer(e)
                        outBuf.setValue(buffer)
                        UNIFFI_CALLBACK_ERROR
                    }
                    {%- else %}
                    val buffer = this.{{ method_name }}(cb, argsData, argsLen)
                    // Success
                    outBuf.setValue(buffer)
                    UNIFFI_CALLBACK_SUCCESS
                    {%- endmatch %}
                } catch (e: Throwable) {
                    try {
                        outBuf.setValue({{ Type::String.borrow()|ffi_converter_name }}.lower(e.toString()))
                    } catch (e: Throwable) {
                    }
                    UNIFFI_CALLBACK_UNEXPECTED_ERROR
                }
            }
            {% endfor %}
            else -> {
                try {
                    outBuf.setValue({{ Type::String.borrow()|ffi_converter_name }}.lower("Invalid Callback index"))
                } catch (e: Throwable) {
                }
                UNIFFI_CALLBACK_UNEXPECTED_ERROR
            }
        }
    }

    {% for meth in cbi.methods() -%}
    {% let method_name = format!("invoke_{}", meth.name())|fn_name %}
    private fun {{ method_name }}(kotlinCallbackInterface: {{ type_name }}, argsData: UBytePointer, argsLen: Int): RustBuffer {
        {#- Unpacking args from the RustBuffer #}
        {%- if meth.arguments().len() != 0 -%}
        {#- Calling the concrete callback object #}
        val source = argsData.asSource(argsLen.toLong())
        return kotlinCallbackInterface.{{ meth.name()|fn_name }}(
            {% for arg in meth.arguments() -%}
            {{ arg|read_fn }}(source)
            {%- if !loop.last %}, {% endif %}
            {% endfor -%}
        )
        {% else %}
        return kotlinCallbackInterface.{{ meth.name()|fn_name }}()
        {% endif -%}

        {#- Packing up the return value into a RustBuffer #}
        {%- match meth.return_type() -%}
        {%- when Some with (return_type) -%}
            .let { {{ return_type|ffi_converter_name }}.lowerIntoRustBuffer(it) }
            {%- else -%}
            .let { emptyRustBuffer() }
            {% endmatch -%}
    }
    {% endfor %}
}
