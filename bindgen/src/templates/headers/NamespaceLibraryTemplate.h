{% for func in ci|iter_ffi_function_definitions %}
    {%- match func.return_type() -%}
        {% when Some with (type_) -%}{{- type_|ffi_header_type_name -}}
        {% when None -%}void
    {%- endmatch -%}
    {# +#} {{ func.name() -}}(
    {%- if func.arguments().len() > 0 %}
        {%- for arg in func.arguments() %}
            {{- arg.type_().borrow()|ffi_header_type_name }} {{ arg.name() -}}_
            {%- if !loop.last || func.has_rust_call_status_arg() %}, {% endif %}
        {%- endfor %}
        {%- if func.has_rust_call_status_arg() %}RustCallStatus *_Nonnull out_status{% endif %}
    {%- else -%}
        {%- if func.has_rust_call_status_arg() %}RustCallStatus *_Nonnull out_status{%- else %}void{% endif %}
    {%- endif -%}
    );
{% endfor +%}
