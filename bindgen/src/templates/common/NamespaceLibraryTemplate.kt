internal expect object UniFFILib {
    {% for func in ci|iter_ffi_function_definitions -%}
    fun {{ func.name() }}(
        {%- call kt::arg_list_ffi_decl(func) -%}
    ): {% match func.return_type() %}{% when Some with (return_type) %}{{ return_type.borrow()|ffi_type_name }}{% when None %}Unit{% endmatch %}
    {% endfor %}
}
