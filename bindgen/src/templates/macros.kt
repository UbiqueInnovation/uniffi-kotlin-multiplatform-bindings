
{#
// Template to call into rust. Used in several places.
// Variable names in `arg_list` should match up with arg lists
// passed to rust via `arg_list_lowered`
#}

{%- macro check_rust_buffer_length(length) -%}
    require({{ length }} <= Int.MAX_VALUE) {
        val length = {{ length }}
        "cannot handle RustBuffer longer than Int.MAX_VALUE bytes: length is $length"
    }
{%- endmacro %}

{%- macro to_ffi_call(func) -%}
    {%- if func.self_type().is_some() %}
    callWithHandle {
        {%- call to_raw_ffi_call(func) %}{% endcall %}
    }
    {% else %}
        {%- call to_raw_ffi_call(func) %}{% endcall %}
    {% endif %}
{%- endmacro %}

{%- macro to_raw_ffi_call(func) -%}
    {%- match func.throws_type() %}
    {%- when Some with (e) %}
    uniffiRustCallWithError({{ e|type_name(ci) }}ErrorHandler)
    {%- else %}
    uniffiRustCall()
    {%- endmatch %} { _status ->
    UniffiLib.INSTANCE.{{ func.ffi_func().name() }}(
        {% if func.self_type().is_some() %}it, {% endif -%}
        {% call arg_list_lowered(func) %}{% endcall -%}
        _status)!!
}
{%- endmacro -%}

{%- macro func_decl(func_decl, callable, indent) %}
    {%- call docstring(callable, indent) %}{% endcall %}
    {%- match callable.throws_type() -%}
    {%-     when Some(throwable) %}
    {#- On the JVM `@Throws` is what puts the `throws` clause into the class file, and Java
        callers can only catch the exception when the clause is on the method they call. So
        the annotation has to be repeated on the overrides of the JVM/Android actuals,
        otherwise the exception is catchable through the interface but not through the class
        implementing it.

        The override is left unannotated everywhere else. Kotlin/Native rejects the repeated
        (identical) annotation on an override of a commonMain interface as a mismatched
        '@Throws' filter (https://youtrack.jetbrains.com/issue/KT-88548), and annotating the
        commonMain `expect` instead would only move the problem to the native `actual`, which
        must then repeat it. Native overrides inherit the filter from the interface anyway. #}
    {%-         if !func_decl.contains("override") || module_name == "jvm" || module_name == "android" %}
    @Throws({{ throwable|type_name(ci) }}::class {%- if callable.is_async() -%},kotlin.coroutines.cancellation.CancellationException::class{%- endif -%})
    {%-         endif %}
    {%-     else -%}
    {%- endmatch -%}
    {%- if callable.is_async() %}
    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    {{ func_decl }} suspend fun {{ callable.name()|fn_name }}(
        {%- call arg_list(callable, !callable.self_type().is_some()) %}{% endcall -%}
    ){% match callable.return_type() %}{% when Some with (return_type) %} : {{ return_type|type_name(ci) }}{% when None %}{%- endmatch %}
    {%- else -%}
    {{ func_decl }} fun {{ callable.name()|fn_name }}(
        {%- call arg_list(callable, !callable.self_type().is_some()) %}{% endcall -%}
    ){%- match callable.return_type() -%}
    {%-         when Some with (return_type) -%}
        : {{ return_type|type_name(ci) }}
    {%-         else %}
    {%-     endmatch %}
    {% endif %}
{% endmacro %}

{%- macro func_decl_with_body(func_decl, callable, indent) %}
    {%- call docstring(callable, indent) %}{% endcall %}
    {%- match callable.throws_type() -%}
    {%-     when Some(throwable) %}
    {#- On the JVM `@Throws` is what puts the `throws` clause into the class file, and Java
        callers can only catch the exception when the clause is on the method they call. So
        the annotation has to be repeated on the overrides of the JVM/Android actuals,
        otherwise the exception is catchable through the interface but not through the class
        implementing it.

        The override is left unannotated everywhere else. Kotlin/Native rejects the repeated
        (identical) annotation on an override of a commonMain interface as a mismatched
        '@Throws' filter (https://youtrack.jetbrains.com/issue/KT-88548), and annotating the
        commonMain `expect` instead would only move the problem to the native `actual`, which
        must then repeat it. Native overrides inherit the filter from the interface anyway. #}
    {%-         if !func_decl.contains("override") || module_name == "jvm" || module_name == "android" %}
    @Throws({{ throwable|type_name(ci) }}::class {%- if callable.is_async() -%},kotlin.coroutines.cancellation.CancellationException::class{%- endif -%})
    {%-         endif %}
    {%-     else -%}
    {%- endmatch -%}
    {%- if callable.is_async() %}
    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    {{ func_decl }} suspend fun {{ callable.name()|fn_name }}(
        {%- call arg_list(callable, false) %}{% endcall -%}
    ){% match callable.return_type() %}{% when Some with (return_type) %} : {{ return_type|type_name(ci) }}{% when None %}{%- endmatch %} {
        return {% call call_async(callable) %}{% endcall %}
    }
    {%- else -%}
    {{ func_decl }} fun {{ callable.name()|fn_name }}(
        {%- call arg_list(callable, false) %}{% endcall -%}
    ){%- match callable.return_type() -%}
    {%-         when Some with (return_type) -%}
        : {{ return_type|type_name(ci) }} {
            return {{ return_type|lift_fn }}({% call to_ffi_call(callable) %}{% endcall %})
    }
    {%-         when None %}
        = {% call to_ffi_call(callable) %}{% endcall %}
    {%-     endmatch %}
    {% endif %}
{% endmacro %}

{%- macro call_async(callable) -%}
    uniffiRustCallAsync(
{%- if callable.self_type().is_some() %}
        callWithHandle { thisHandle ->
            UniffiLib.INSTANCE.{{ callable.ffi_func().name() }}(
                thisHandle,
                {% call arg_list_lowered(callable) %}{% endcall %}
            )!!
        },
{%- else %}
        UniffiLib.INSTANCE.{{ callable.ffi_func().name() }}({% call arg_list_lowered(callable) %}{% endcall %})!!,
{%- endif %}
        {{ callable|async_poll(ci) }},
        {{ callable|async_complete(ci) }},
        {{ callable|async_free(ci) }},
        {{ callable|async_cancel(ci) }},
        // lift function
        {%- match callable.return_type() %}
        {%- when Some(return_type) %}
        { {{ return_type|lift_fn }}(it!!) },
        {%- when None %}
        { Unit },
        {% endmatch %}
        // Error FFI converter
        {%- match callable.throws_type() %}
        {%- when Some(e) %}
        {{ e|type_name(ci) }}ErrorHandler,
        {%- when None %}
        UniffiNullRustCallStatusErrorHandler,
        {%- endmatch %}
    )
{%- endmacro %}

{%- macro arg_list_lowered(func) %}
    {%- for arg in func.arguments() %}
        {{- arg|lower_fn_for_arg }}({{ arg.name()|var_name }}),
    {%- endfor %}
{%- endmacro -%}

{#-
// Arglist as used in kotlin declarations of methods, functions and constructors.
// If is_decl, then default values be specified.
// Note the var_name and type_name filters.
-#}

{% macro arg_list(func, is_decl) %}
{%- for arg in func.arguments() -%}
        {{ arg.name()|var_name }}: {{ arg|type_name(ci) }}
{%-     if is_decl %}
{%-         match arg.default_value() %}
{%-             when Some with(literal) %} = {{ literal|render_default(arg, ci) }}
{%-             else %}
{%-         endmatch %}
{%-     endif %}
{%-     if !loop.last %}, {% endif -%}
{%- endfor %}
{%- endmacro %}

{#-
// Arglist as used in the UniffiLib function declarations.
// Note unfiltered name but ffi_type_name filters.
-#}
{%- macro arg_list_ffi_decl(func) %}
    {%- for arg in func.arguments() %}
        {{- arg.name()|var_name }}: {{ arg.type_().borrow()|ffi_type_name_by_value(ci) -}},
    {%- endfor %}
    {%- if func.has_rust_call_status_arg() %}uniffiCallStatus: UniffiRustCallStatus, {% endif %}
{%- endmacro -%}

{%- macro arg_list_ffi_decl_for_ffi_function(func) %}
    {%- for arg in func.arguments() %}
        {{- arg.name()|var_name }}: {{ arg.type_().borrow()|ffi_type_name_for_ffi_function(ci) -}},
    {%- endfor %}
    {%- if func.has_rust_call_status_arg() %}uniffiCallStatus: UniffiRustCallStatus, {% endif %}
{%- endmacro -%}

{%- macro arg_list_ffi_call(func) %}
    {%- for arg in func.arguments() %}
        {%- if arg.type_().borrow()|is_callback -%}
        {{ arg.name()|var_name }} as cinterop.{{ arg.type_().borrow()|ffi_type_name_for_ffi_callback }}
        {%- else if arg.type_().borrow()|is_rustbuffer -%}
        {{- arg.name()|var_name }} as CValue<cinterop.RustBuffer>
        {%- else if arg.type_().borrow()|is_foreignbytes -%}
        {{- arg.name()|var_name }} as CValue<cinterop.ForeignBytes>
        {%- else -%}
        {{- arg.name()|var_name }}
        {%- endif -%}
        {%- if arg.type_().borrow()|is_pointer_type -%}
        ?.inner
        {%- endif -%},
    {%- endfor %}
    {%- if func.has_rust_call_status_arg() %}uniffiCallStatus.reinterpret(), {% endif %}
{%- endmacro -%}

{% macro field_name(field, field_num) %}
{%- if field.name().is_empty() -%}
v{{- field_num -}}
{%- else -%}
{{ field.name()|var_name }}
{%- endif -%}
{%- endmacro %}

{% macro field_name_unquoted(field, field_num) %}
{%- if field.name().is_empty() -%}
v{{- field_num -}}
{%- else -%}
{{ field.name()|var_name|unquote }}
{%- endif -%}
{%- endmacro %}

{% macro field_name_unquoted_unescaped(field, field_num) %}
{%- if field.name().is_empty() -%}
v{{- field_num -}}
{%- else -%}
{{ field.name()|var_name_raw_noescape }}
{%- endif -%}
{%- endmacro %}

 // Macro for destroying fields
{%- macro destroy_fields(member) %}
    Disposable.destroy(
        {%- for field in member.fields() %}
            this.{%- call field_name(field, loop.index) %}{% endcall -%},
        {% endfor -%}
    )
{%- endmacro -%}

{%- macro docstring_value(maybe_docstring, indent_spaces) %}
{%- match maybe_docstring %}
{%- when Some(docstring) %}
{{ docstring|docstring(indent_spaces) }}
{%- else %}
{%- endmatch %}
{%- endmacro %}

{%- macro docstring(defn, indent_spaces) %}
{%- call docstring_value(defn.docstring(), indent_spaces) %}{% endcall %}
{%- endmacro %}
