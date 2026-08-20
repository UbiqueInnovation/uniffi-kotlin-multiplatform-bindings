
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

{#-
// Since uniffi 0.31 a method's receiver is not necessarily an object: records and enums
// can carry methods too, and those lower their `self` into a `RustBuffer` like any other
// value instead of passing a handle. Only the object case needs `callWithHandle`.
//
// A record/enum method never has its body on the type itself - see `self_shim_expect` -
// so the receiver is always in scope under the name `uniffiSelf` when we get here.
-#}
{%- macro to_ffi_call(func) -%}
    {%- match func.self_type() %}
    {%- when Some(Type::Object { .. }) %}
    callWithHandle {
        {%- call to_raw_ffi_call(func) %}{% endcall %}
    }
    {% else %}
        {%- call to_raw_ffi_call(func) %}{% endcall %}
    {% endmatch %}
{%- endmacro %}

{%- macro to_raw_ffi_call(func) -%}
    {%- match func.throws_type() %}
    {%- when Some with (e) %}
    uniffiRustCallWithError({{ e|type_name(ci) }}ErrorHandler)
    {%- else %}
    uniffiRustCall()
    {%- endmatch %} { _status ->
    UniffiLib.INSTANCE.{{ func.ffi_func().name() }}(
        {%- match func.self_type() %}
        {%- when Some(Type::Object { .. }) %}
        it,
        {%- when Some(self_type) %}
        {{ self_type|lower_fn }}(uniffiSelf),
        {%- when None %}
        {%- endmatch %}
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
{%- match callable.self_type() %}
{%- when Some(Type::Object { .. }) %}
        callWithHandle { thisHandle ->
            UniffiLib.INSTANCE.{{ callable.ffi_func().name() }}(
                thisHandle,
                {% call arg_list_lowered(callable) %}{% endcall %}
            )!!
        },
{%- when Some(self_type) %}
        UniffiLib.INSTANCE.{{ callable.ffi_func().name() }}(
            {{ self_type|lower_fn }}(uniffiSelf),
            {% call arg_list_lowered(callable) %}{% endcall %}
        )!!,
{%- when None %}
        UniffiLib.INSTANCE.{{ callable.ffi_func().name() }}({% call arg_list_lowered(callable) %}{% endcall %})!!,
{%- endmatch %}
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

{#-
// ---------------------------------------------------------------------------
// Methods and uniffi trait exports on records and enums (uniffi 0.31).
//
// An object's class is `expect`/`actual`, so its methods can simply have their
// bodies in the platform source sets. A record or enum is a plain `data class` /
// `enum class` / `sealed class` declared once in `commonMain`, which has no
// access to `UniffiLib` - so a method on one cannot be written there directly.
//
// Each such method is therefore split in two:
//
//   * the member function stays on the type in `commonMain` and does nothing but
//     delegate to a top-level `internal expect` shim, so consumers see an ordinary
//     method (and `toString`/`equals`/`hashCode`/`compareTo` stay real overrides);
//   * the shim's `actual` is emitted next to the type's `FfiConverter` in each
//     platform source set, where `UniffiLib` is in scope.
//
// The shim takes the receiver as its first parameter, always named `uniffiSelf` -
// which is the name `to_raw_ffi_call` lowers for a non-object receiver.
//
// Shims are named after the FFI symbol they wrap. That name is already unique
// across the whole library, so two types cannot collide however their methods are
// named.
-#}

{%- macro self_shim_name(callable) %}uniffiSelfCall_{{ callable.ffi_func().name() }}{% endmacro %}

{#- The arguments of a shim, receiver first. -#}
{%- macro self_shim_args(callable, self_type_name) %}
        uniffiSelf: {{ self_type_name }},
        {%- for arg in callable.arguments() %}
        {{ arg.name()|var_name }}: {{ arg|type_name(ci) }},
        {%- endfor %}
{%- endmacro %}

{#- Forwarding the receiver and arguments of a member function on to its shim. -#}
{%- macro self_shim_call(callable, receiver) %}
{%- call self_shim_name(callable) %}{% endcall %}({{ receiver }}
    {%- for arg in callable.arguments() %}, {{ arg.name()|var_name }}{% endfor %})
{%- endmacro %}

{#- `internal expect fun uniffiSelfCall_...(...)`, for `commonMain`. -#}
{%- macro self_shim_expect(callable, self_type_name) %}
internal expect {% if callable.is_async() %}suspend {% endif %}fun {% call self_shim_name(callable) %}{% endcall %}(
    {%- call self_shim_args(callable, self_type_name) %}{% endcall %}
){%- match callable.return_type() %}{% when Some(return_type) %}: {{ return_type|type_name(ci) }}{% when None %}{%- endmatch %}
{% endmacro %}

{#- `internal actual fun uniffiSelfCall_...(...) { ... }`, for a platform source set. -#}
{%- macro self_shim_actual(callable, self_type_name) %}
{%- if callable.is_async() %}
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
{%- endif %}
internal actual {% if callable.is_async() %}suspend {% endif %}fun {% call self_shim_name(callable) %}{% endcall %}(
    {%- call self_shim_args(callable, self_type_name) %}{% endcall %}
){%- match callable.return_type() %}{% when Some(return_type) %}: {{ return_type|type_name(ci) }}{% when None %}{%- endmatch %} {
    {%- if callable.is_async() %}
    return {% call call_async(callable) %}{% endcall %}
    {%- else %}
    {%- match callable.return_type() %}
    {%- when Some(return_type) %}
    return {{ return_type|lift_fn }}({% call to_ffi_call(callable) %}{% endcall %})
    {%- when None %}
    {% call to_ffi_call(callable) %}{% endcall %}
    {%- endmatch %}
    {%- endif %}
}
{% endmacro %}

{#- The member function itself, on the record/enum in `commonMain`. -#}
{%- macro self_method_decl(callable, indent) %}
    {%- call docstring(callable, indent) %}{% endcall %}
    {%- match callable.throws_type() %}
    {%-     when Some(throwable) %}
    @Throws({{ throwable|type_name(ci) }}::class {%- if callable.is_async() -%},kotlin.coroutines.cancellation.CancellationException::class{%- endif -%})
    {%-     else -%}
    {%- endmatch %}
    {% if callable.is_async() %}suspend {% endif %}fun {{ callable.name()|fn_name }}(
        {%- call arg_list(callable, true) %}{% endcall -%}
    ){%- match callable.return_type() %}{% when Some(return_type) %}: {{ return_type|type_name(ci) }}{% when None %}{%- endmatch %} =
        {% call self_shim_call(callable, "this") %}{% endcall %}
{% endmacro %}

{#-
// The `expect` shims a record/enum needs: one per exported method, one per uniffi
// trait method it actually renders. Emitted at top level, right after the type.
//
// `flat` selects the reduced trait set a Kotlin `enum class` can accept - see
// `self_uniffi_trait_impls`.
-#}
{%- macro self_shims_expect(methods, uniffi_trait_methods, self_type_name, flat) %}
{%- for meth in methods %}
{%- call self_shim_expect(meth, self_type_name) %}{% endcall %}
{%- endfor %}
{%- call self_uniffi_trait_shims(uniffi_trait_methods, self_type_name, flat, "expect") %}{% endcall %}
{%- endmacro %}

{#- The matching `actual` shims, for a platform source set. -#}
{%- macro self_shims_actual(methods, uniffi_trait_methods, self_type_name, flat) %}
{%- for meth in methods %}
{%- call self_shim_actual(meth, self_type_name) %}{% endcall %}
{%- endfor %}
{%- call self_uniffi_trait_shims(uniffi_trait_methods, self_type_name, flat, "actual") %}{% endcall %}
{%- endmacro %}

{%- macro self_uniffi_trait_shims(uniffi_trait_methods, self_type_name, flat, kind) %}
{%- if let Some(fmt) = uniffi_trait_methods.display_fmt.clone().or(uniffi_trait_methods.debug_fmt.clone()) %}
{%- if kind == "expect" %}{% call self_shim_expect(fmt, self_type_name) %}{% endcall %}
{%- else %}{% call self_shim_actual(fmt, self_type_name) %}{% endcall %}{% endif %}
{%- endif %}
{%- if !flat %}
{%- if let Some(eq) = uniffi_trait_methods.eq_eq.clone() %}
{%- if kind == "expect" %}{% call self_shim_expect(eq, self_type_name) %}{% endcall %}
{%- else %}{% call self_shim_actual(eq, self_type_name) %}{% endcall %}{% endif %}
{%- endif %}
{%- if let Some(hash) = uniffi_trait_methods.hash_hash.clone() %}
{%- if kind == "expect" %}{% call self_shim_expect(hash, self_type_name) %}{% endcall %}
{%- else %}{% call self_shim_actual(hash, self_type_name) %}{% endcall %}{% endif %}
{%- endif %}
{%- if let Some(cmp) = uniffi_trait_methods.ord_cmp.clone() %}
{%- if kind == "expect" %}{% call self_shim_expect(cmp, self_type_name) %}{% endcall %}
{%- else %}{% call self_shim_actual(cmp, self_type_name) %}{% endcall %}{% endif %}
{%- endif %}
{%- endif %}
{%- endmacro %}

{#-
// `Display`/`Debug` -> `toString`, `Eq` -> `equals`, `Hash` -> `hashCode`,
// `Ord` -> `compareTo`, for a record or enum. Each override delegates to its shim.
//
// `flat` is set for a Kotlin `enum class`, where `equals`, `hashCode` and `compareTo`
// are final on `kotlin.Enum` and cannot be overridden. Those three are dropped there:
// a fieldless Rust enum's derived `Eq`/`Hash`/`Ord` agree with what Kotlin already
// gives an enum entry anyway (identity, and ordering by declaration order), the one
// exception being explicit out-of-order discriminants, which `#[derive(Ord)]` compares
// by value where Kotlin compares by ordinal.
-#}
{%- macro self_uniffi_trait_impls(uniffi_trait_methods, self_type_name, flat) %}
{#- We have 2 display traits, kotlin has 1. Prefer `Display` but use `Debug` otherwise -#}
{%- if let Some(fmt) = uniffi_trait_methods.display_fmt.clone().or(uniffi_trait_methods.debug_fmt.clone()) %}
    // The local Rust `Display`/`Debug` implementation.
    override fun toString(): String =
        {% call self_shim_call(fmt, "this") %}{% endcall %}
{%- endif %}
{%- if !flat %}
{%- if let Some(eq) = uniffi_trait_methods.eq_eq.clone() %}
    // The local Rust `Eq` implementation - only `eq` is used.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is {{ self_type_name }}) return false
        return {% call self_shim_call(eq, "this") %}{% endcall %}
    }
{%- endif %}
{%- if let Some(hash) = uniffi_trait_methods.hash_hash.clone() %}
    // The local Rust `Hash` implementation.
    override fun hashCode(): Int =
        {% call self_shim_call(hash, "this") %}{% endcall %}.toInt()
{%- endif %}
{%- if let Some(cmp) = uniffi_trait_methods.ord_cmp.clone() %}
    // The local Rust `Ord` implementation.
    override fun compareTo(other: {{ self_type_name }}): Int =
        {% call self_shim_call(cmp, "this") %}{% endcall %}.toInt()
{%- endif %}
{%- endif %}
{%- endmacro %}
