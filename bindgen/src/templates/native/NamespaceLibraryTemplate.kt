actual internal object UniFFILib {
    init {
        {% let initialization_fns = self.initialization_fns() %}
        {%- if !initialization_fns.is_empty() -%}
        {% for fn in initialization_fns -%}
        {{ fn }}(this)
        {% endfor -%}
        {% endif %}
    }

    {% for func in ci|iter_ffi_function_definitions -%}
    actual fun {{ func.name() }}(
    {%- call kt::arg_list_ffi_decl(func) %}
    ){%- match func.return_type() -%}{%- when Some with (type_) %}: {{ type_.borrow()|ffi_type_name }}{% when None %}: Unit{% endmatch %} =
    requireNotNull({{ ci.namespace() }}.cinterop.{{ func.name() }}(
    {%- call kt::arg_list_ffi_call(func) %}
    ))

    {% endfor %}
}
