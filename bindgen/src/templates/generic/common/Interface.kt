
{%- call kt::docstring_value(interface_docstring, 0) %}{% endcall %}
interface {{ interface_name }} {
    {% for meth in methods.iter() -%}
    {%- call kt::docstring(meth, 4) %}{% endcall %}
    {%- match meth.throws_type() -%}
        {%-     when Some(throwable) %}
        @Throws({{ throwable|type_name(ci) }}::class {%- if meth.is_async() -%},kotlin.coroutines.cancellation.CancellationException::class{%- endif -%})
        {%-     else -%}
        {%- endmatch -%}
    {% if meth.is_async() -%}suspend {% endif -%}
    fun {{ meth.name()|fn_name }}({% call kt::arg_list(meth, true) %}{% endcall %})
    {%- match meth.return_type() -%}
    {%- when Some with (return_type) %}: {{ return_type|type_name(ci) -}}
    {%- else -%}
    {%- endmatch %}
    {% endfor %}
    companion object
}

