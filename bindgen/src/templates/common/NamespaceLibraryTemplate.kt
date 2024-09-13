
// Define FFI callback types
{%- for def in ci.ffi_definitions() %}
{%- match def %}
{%- when FfiDefinition::CallbackFunction(callback) %}
{%- if callback.name().contains("ForeignFuture") %}
internal expect fun create{{ callback.name()|ffi_callback_name }}Callback(
): Any
{%- endif %}
{%- when FfiDefinition::Struct(ffi_struct) %}
internal expect class {{ ffi_struct.name()|ffi_struct_name }}(
    {%- for field in ffi_struct.fields() %}
    {{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }} ,
    {%- endfor %}
)
{% for field in ffi_struct.fields() %}
internal expect var {{ ffi_struct.name()|ffi_struct_name }}.{{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }}
{% endfor %}

internal expect fun {{ ffi_struct.name()|ffi_struct_name }}.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }})
internal expect fun {{ ffi_struct.name()|ffi_struct_name }}.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue)

internal expect class {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue(
    {%- for field in ffi_struct.fields() %}
    {{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }},
    {%- endfor %}
)
{% for field in ffi_struct.fields() %}
internal expect var {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue.{{ field.name()|var_name }}: {{ field.type_().borrow()|ffi_type_name_for_ffi_struct }}
{% endfor %}

internal expect fun {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }})
internal expect fun {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue.uniffiSetValue(other: {{ ffi_struct.name()|ffi_struct_name }}UniffiByValue)
{%- when FfiDefinition::Function(_) %}
{# functions are handled below #}
{%- endmatch %}
{%- endfor %}


// A JNA Library to expose the extern-C FFI definitions.
// This is an implementation detail which will be called internally by the public API.

internal expect interface UniffiLib {
    companion object {
        internal val INSTANCE: UniffiLib

        {% if ci.contains_object_types() %}
        // The Cleaner for the whole library
        internal val CLEANER: UniffiCleaner
        {%- endif %}
    }

    {% for func in ci.iter_ffi_function_definitions() -%}
    fun {{ func.name() }}(
        {%- call kt::arg_list_ffi_decl(func) %}
    ): {% match func.return_type() %}{% when Some with (return_type) %}{{ return_type.borrow()|ffi_type_name_by_value }}{% when None %}Unit{% endmatch %}
    {% endfor %}
}
