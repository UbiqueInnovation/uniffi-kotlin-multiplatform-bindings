{%- let cbi = ci|get_callback_interface_definition(name) %}
{%- let foreign_callback_name = format!("ForeignCallback{}", name) %}

{% include "CallbackInterfaceImpl.kt" %}

interface {{ type_name }} {
    {% for meth in cbi.methods() -%}
    fun {{ meth.name()|fn_name }}({% call kt::arg_list_decl(meth) %})
    {%- match meth.return_type() -%}
    {%- when Some with (return_type) %}: {{ return_type|type_name -}}
    {%- else -%}
    {%- endmatch %}
    {% endfor %}
}

object {{ ffi_converter_name }}: FfiConverterCallbackInterface<{{ type_name }}>() {
    // prevent the callback from being GC'ed
    private val foreignCallback = {{ foreign_callback_name }}.toForeignCallback()

    override fun register(lib: UniFFILib) {
        rustCall { status ->
            lib.{{ cbi.ffi_init_callback().name() }}(foreignCallback, status)
        }
    }
}

expect fun {{ foreign_callback_name }}.toForeignCallback() : ForeignCallback
