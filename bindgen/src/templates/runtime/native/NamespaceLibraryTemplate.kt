
internal interface UniffiLib {
    companion object {
        internal val INSTANCE: UniffiLib by lazy {
            UniffiLibInstance().also { lib ->
             {% for init_fn in self.initialization_fns() -%}
                {{ init_fn }}(lib)
             {% endfor -%}
             }
        }
        {% if ci.contains_object_types() %}
        // The Cleaner for the whole library
        internal val CLEANER: UniffiCleaner by lazy {
            UniffiCleaner.create()
        }
        {%- endif %}
    }

    {% for func in ci.iter_ffi_function_definitions() -%}
    fun {{ func.name() }}(
        {%- call kt::arg_list_ffi_decl_for_ffi_function(func) %}{% endcall %}
    ): {% match func.return_type() %}{% when Some with (return_type) %}{{ return_type.borrow()|ffi_type_name_for_ffi_function(ci) }}{% when None %}Unit{% endmatch %}
    {% endfor %}
}

internal class UniffiLibInstance: UniffiLib {
    {% for func in ci.iter_ffi_function_definitions() -%}
    override fun {{ func.name() }}(
        {%- call kt::arg_list_ffi_decl_for_ffi_function(func) %}{% endcall %}
    ): {% match func.return_type() %}{% when Some with (return_type) %}{{ return_type.borrow()|ffi_type_name_for_ffi_function(ci) }}{% when None %}Unit{% endmatch %}
        = cinterop.{{ func.name() }}({%- call kt::arg_list_ffi_call(func) %}{% endcall %})
          {%- match func.return_type() -%}
          {%- when Some with (return_type) -%}
          {%- if return_type.borrow()|is_pointer_type -%}
            ?.let { Pointer(it) }
          {%- endif -%}
          {%- when None -%}
          {%- endmatch %}
    
    {% endfor %}
}
