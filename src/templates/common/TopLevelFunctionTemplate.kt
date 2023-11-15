{%- if func.is_async() %}
{%- match func.throws_type() -%}
{%- when Some with (throwable) %}
@Throws({{ throwable|error_type_name }}::class, CancellationException::class)
{%- else -%}
{%- endmatch %}

@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
suspend fun {{ func.name()|fn_name }}({%- call kt::arg_list_decl(func) -%}){% match func.return_type() %}{% when Some with (return_type) %} : {{ return_type|type_name }}{% when None %}{%- endmatch %} {
    return uniffiRustCallAsync(
        UniFFILib.{{ func.ffi_func().name() }}({% call kt::arg_list_lowered(func) %}),
        {{ func|async_poll(ci) }},
        {{ func|async_complete(ci) }},
        {{ func|async_free(ci) }},
        // lift function
        {%- match func.return_type() %}
        {%- when Some(return_type) %}
        { {{ return_type|lift_fn }}(it) },
        {%- when None %}
        { Unit },
        {% endmatch %}
        // Error FFI converter
        {%- match func.throws_type() %}
        {%- when Some(e) %}
        {{ e|error_type_name }}.ErrorHandler,
        {%- when None %}
                NullCallStatusErrorHandler,
        {%- endmatch %}
    )
}

{%- else %} {# if func.is_async() #}

{%- match func.throws_type() -%}
{%- when Some with (throwable) %}
@Throws({{ throwable|error_type_name }}::class)
{%- else -%}
{%- endmatch %}
{%- match func.return_type() -%}
{%- when Some with (return_type) %}

fun {{ func.name()|fn_name }}({%- call kt::arg_list_decl(func) -%}): {{ return_type|type_name }} {
    return {{ return_type|lift_fn }}({% call kt::to_ffi_call(func) %})
}

{% when None %}

fun {{ func.name()|fn_name }}({% call kt::arg_list_decl(func) %}) =
    {% call kt::to_ffi_call(func) %}
{% endmatch %}
{%- endif %}
